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
 * <p>Ansvar: Kör samma scenario som headless-läget mot en riktig SQLite-databas med Flyway-schemat,
 * och bevisar att tjänsterna följer samma regler utan gränssnitt: hela drop-in-flödet går igenom,
 * och de två otillåtna operationerna ger samma språknycklar som gränssnittet visar.</p>
 * <p>Profilen {@code headless} aktiveras medvetet inte, så att {@code HeadlessRunner} inte körs vid
 * uppstart. Testet anropar {@link DropInScenario} direkt och kontrollerar resultatet i stället för utskriften.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class HeadlessDropInScenarioIntegrationTest {

    private static final String START_CANCELLED = "Starta avbruten order";
    private static final String UNKNOWN_VEHICLE = "Skapa order för okänt fordon";

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
        // Fakturor som fanns innan (seed-datan innehåller redan en betald faktura)
        Set<Integer> invoicesBefore = invoiceService.findAll().stream()
                .map(Invoice::getId)
                .collect(Collectors.toSet());

        List<StepResult> results = scenario.run();

        // Alla steg kördes, och alla blev som förväntat
        assertEquals(11, results.size(), () -> "Scenario stopped early: " + describe(results));
        assertTrue(results.stream().allMatch(StepResult::isAsExpected),
                () -> "Not all steps as expected: " + describe(results));

        // Huvudflödet: scenariot skapade exakt en faktura, och den är betald
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

        // Otillåten statusövergång: samma nyckel som ErrorFacade visar i gränssnittet
        ErrorReport transition = errorOf(results, START_CANCELLED);

        assertEquals("error.statusTransitionMsg", transition.getMessageKey());
        assertEquals(ErrorReport.Severity.EXPECTED, transition.getSeverity());

        // Okänt fordon
        ErrorReport notFound = errorOf(results, UNKNOWN_VEHICLE);

        assertEquals("error.notFound", notFound.getMessageKey());
        assertEquals(ErrorReport.Severity.EXPECTED, notFound.getSeverity());
    }

    // Hämtar via namn i stället för index, så att testet tål nya steg i mitten
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
