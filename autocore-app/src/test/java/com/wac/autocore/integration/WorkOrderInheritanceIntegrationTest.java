package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.PlannedWorkOrder;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.WorkOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>WorkOrderInheritanceIntegrationTest</b>
 * <p>Responsibility: Proves that JPA inheritance with SINGLE_TABLE reads the {@code type} column correctly against the
 * real V7 schema: a row with {@code type = 'PLANNED'} becomes a {@link PlannedWorkOrder},
 * and the repository's queries against {@link WorkOrder} return the subclass.</p>
 * <p>The row is written with SQL and not via the service, so the test mimics data that already
 * existed in the database and does not just make a round trip through our own code.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class WorkOrderInheritanceIntegrationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("work-order-inheritance.db").toAbsolutePath());
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Test
    @DisplayName("Existing row with type PLANNED loads as PlannedWorkOrder")
    void existingPlannedRowLoadsAsPlannedWorkOrder() {
        int id = insertWorkOrderRow("PLANNED", WorkOrderStatus.CONFIRMED);

        WorkOrder loaded = workOrderRepository.findById(id)
                .orElseThrow(() -> new AssertionError("Work order " + id + " should exist"));

        assertInstanceOf(PlannedWorkOrder.class, loaded);
        assertEquals(WorkOrderStatus.CONFIRMED, loaded.getStatus());
    }

    @Test
    @DisplayName("findCompletedNotInvoiced returns PLANNED rows as PlannedWorkOrder")
    void findCompletedNotInvoicedIsPolymorphic() {
        int id = insertWorkOrderRow("PLANNED", WorkOrderStatus.COMPLETED);

        List<WorkOrder> result = workOrderRepository.findCompletedNotInvoiced();

        WorkOrder found = result.stream()
                .filter(workOrder -> workOrder.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Work order " + id + " should be in the result, got ids "
                        + result.stream().map(WorkOrder::getId).collect(Collectors.toList())));

        assertInstanceOf(PlannedWorkOrder.class, found);
    }

    /* Writes a work order directly into the table. booking_id, vehicle_id and mechanic_id are left NULL
     * (allowed since V7), otherwise foreign_keys=true stops the row. The id is read with MAX(id) and not
     * with last_insert_rowid(), because JdbcTemplate may get a new connection from the pool between the calls. */
    private int insertWorkOrderRow(String type, WorkOrderStatus status) {
        jdbcTemplate.update("INSERT INTO work_order (type, status) VALUES (?, ?)", type, status.name());
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM work_order", Integer.class);
        assertNotNull(id, "Insert should have produced a work order id");
        return id;
    }
}
