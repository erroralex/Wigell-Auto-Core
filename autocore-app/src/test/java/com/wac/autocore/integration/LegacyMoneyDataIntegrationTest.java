package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.InvoiceRepo;
import com.wac.autocore.repository.ServiceItemRepository;
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

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * <b>LegacyMoneyDataIntegrationTest</b>
 * <p>Ansvar: Bevisar att belopp som redan ligger i en gammal databas (REAL-värden med flyttalsbrus,
 * och NULL i invoice_line) läses som exakta Money-värden. Raderna skrivs med SQL och inte via
 * servicen, så att de efterliknar data från före Money.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class LegacyMoneyDataIntegrationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("legacy-money.db").toAbsolutePath());
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InvoiceRepo invoiceRepo;
    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Test
    @DisplayName("Old invoice with ordinary REAL values shows the same amounts")
    void oldInvoiceKeepsItsAmounts() {
        int invoiceId = insertInvoice(1097.5, 109.75, 987.75);

        Invoice invoice = load(invoiceId);

        assertMoney(Money.of("1097.50"), invoice.getAmount());
        assertMoney(Money.of("109.75"), invoice.getDiscount());
        assertMoney(Money.of("987.75"), invoice.getTotalAmount());
    }

    @Test
    @DisplayName("Floating-point noise in old values is rounded to öre when read")
    void floatingPointNoiseIsRounded() {
        double noisy = 0.1 + 0.2;
        int invoiceId = insertInvoice(noisy, 0.0, noisy);
        insertLine(invoiceId, noisy, 0.0, noisy);

        Invoice invoice = load(invoiceId);

        assertMoney(Money.of("0.30"), invoice.getAmount());
        assertMoney(Money.ZERO, invoice.getDiscount());
        assertMoney(Money.of("0.30"), invoice.getLines().get(0).getAmount());
        assertMoney(Money.of("0.30"), invoice.getLines().get(0).getTotal());
    }

    @Test
    @DisplayName("NULL in invoice_line is read as Money.ZERO")
    void nullLineAmountsAreReadAsZero() {
        int invoiceId = insertInvoice(100.0, 0.0, 100.0);
        jdbcTemplate.update("INSERT INTO invoice_line (invoice_id, service_item_name) VALUES (?, ?)",
                invoiceId, "Legacy line");

        Invoice invoice = load(invoiceId);

        InvoiceLine line = invoice.getLines().get(0);
        assertEquals("Legacy line", line.getServiceItemName());
        assertMoney(Money.ZERO, line.getAmount());
        assertMoney(Money.ZERO, line.getDiscount());
        assertMoney(Money.ZERO, line.getTotal());
    }

    @Test
    @DisplayName("Old service price is read as Money")
    void oldServicePriceIsRead() {
        jdbcTemplate.update(
                "INSERT INTO service_item (name, description, price, estimated_minutes) VALUES (?, ?, ?, ?)",
                "Legacy oil change", "Legacy", 899.0, 45);
        Integer id = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM service_item", Integer.class);
        assertNotNull(id);

        ServiceItem item = serviceItemRepository.findById(id)
                .orElseThrow(() -> new AssertionError("Service item " + id + " should exist"));

        assertMoney(Money.of(899), item.getPrice());
    }

    private Invoice load(int invoiceId) {
        return invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new AssertionError("Invoice " + invoiceId + " should exist"));
    }

    /* work_order_id måste finnas (foreign_keys=true). Id:n läses med MAX(id), eftersom JdbcTemplate
     * kan få en ny anslutning från poolen mellan anropen. */
    private int insertInvoice(double amount, double discount, double total) {
        jdbcTemplate.update("INSERT INTO work_order (type, status) VALUES ('PLANNED', 'COMPLETED')");
        Integer workOrderId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM work_order", Integer.class);

        jdbcTemplate.update(
                "INSERT INTO invoice (work_order_id, invoice_date, amount, discount, total_amount, paid) "
                        + "VALUES (?, '2026-01-15', ?, ?, ?, 0)",
                workOrderId, amount, discount, total);
        Integer invoiceId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM invoice", Integer.class);
        assertNotNull(invoiceId, "Insert should have produced an invoice id");
        return invoiceId;
    }

    private void insertLine(int invoiceId, double amount, double discount, double total) {
        jdbcTemplate.update(
                "INSERT INTO invoice_line (invoice_id, service_item_name, amount, discount, total) "
                        + "VALUES (?, 'Legacy line', ?, ?, ?)",
                invoiceId, amount, discount, total);
    }
}
