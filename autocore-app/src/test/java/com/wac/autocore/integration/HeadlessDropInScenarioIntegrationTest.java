package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.headless.DropInScenario;
import com.wac.autocore.headless.StepResult;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderType;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.WorkOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>HeadlessDropInScenarioIntegrationTest</b>
 * <p>Responsibility: Runs the same scenario as headless mode against a real SQLite database with the Flyway schema,
 * and proves that the services follow the same rules without a UI: the whole drop-in flow goes through,
 * and the two disallowed operations give the same language keys that the UI shows.</p>
 * <p>The {@code headless} profile is deliberately not activated, so {@code HeadlessRunner} does not run at
 * startup. The test calls {@link DropInScenario} directly and checks the result instead of the printout.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class HeadlessDropInScenarioIntegrationTest {

    private static final String START_CANCELLED = "Start cancelled order";
    private static final String UNKNOWN_VEHICLE = "Create order for unknown vehicle";

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("headless-scenario.db").toAbsolutePath());
    }

    @Autowired private DropInScenario scenario;
    @Autowired private InvoiceService invoiceService;
    @Autowired private WorkOrderService workOrderService;

    @Test
    @DisplayName("Drop-in flow runs through the services and expected errors give the UI's i18n keys")
    void runsDropInFlowAndReportsExpectedErrors() {
        // Invoices that existed before (the seed data already contains a paid invoice)
        Set<Integer> invoicesBefore = invoiceService.findAll().stream()
                .map(Invoice::getId)
                .collect(Collectors.toSet());

        List<StepResult> results = scenario.run();

        // All steps ran, and all went as expected
        assertEquals(11, results.size(), () -> "Scenario stopped early: " + describe(results));
        assertTrue(results.stream().allMatch(StepResult::isAsExpected),
                () -> "Not all steps as expected: " + describe(results));

        // Main flow: the scenario created exactly one invoice, and it is paid
        List<Invoice> created = invoiceService.findAll().stream()
                .filter(invoice -> !invoicesBefore.contains(invoice.getId()))
                .collect(Collectors.toList());

        assertEquals(1, created.size(), "Scenario should create exactly one invoice");
        assertTrue(created.get(0).isPaid(), "The new invoice should be paid");

        int workOrderId = created.get(0).getWorkOrderId();
        WorkOrder order = workOrderService.findById(workOrderId)
                .orElseThrow(() -> new AssertionError("Work order " + workOrderId + " should exist"));

        assertEquals(WorkOrderType.DROP_IN, order.getType());
        assertEquals(WorkOrderStatus.COMPLETED, order.getStatus());

        // Disallowed status transition: the same key that ErrorFacade shows in the UI
        ErrorReport transition = errorOf(results, START_CANCELLED);

        assertEquals("error.statusTransitionMsg", transition.getMessageKey());
        assertEquals(ErrorReport.Severity.EXPECTED, transition.getSeverity());

        // Unknown vehicle
        ErrorReport notFound = errorOf(results, UNKNOWN_VEHICLE);

        assertEquals("error.notFound", notFound.getMessageKey());
        assertEquals(ErrorReport.Severity.EXPECTED, notFound.getSeverity());
    }

    // Looks up by name instead of index, so the test tolerates new steps in the middle
    private static ErrorReport errorOf(List<StepResult> results, String stepName) {
        StepResult step = results.stream()
                .filter(result -> result.getName().equals(stepName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Step not found: " + stepName));
        assertNotNull(step.getError(), () -> "Step should have failed: " + stepName);
        return step.getError();
    }

    private static String describe(List<StepResult> results) {
        return results.stream()
                .map(r -> r.getName() + (r.isAsExpected() ? " OK" : " FAILED"
                        + (r.getError() == null ? "" : " (" + r.getError().getMessageKey() + ")")))
                .collect(Collectors.joining(", "));
    }
}
