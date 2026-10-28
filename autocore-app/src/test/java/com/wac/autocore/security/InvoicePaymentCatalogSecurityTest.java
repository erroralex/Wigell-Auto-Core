package com.wac.autocore.security;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.MechanicService;
import com.wac.autocore.service.PaymentService;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.testsupport.WithAdmin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext

class InvoicePaymentCatalogSecurityTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("security.db").toAbsolutePath());
    }

    @Autowired InvoiceService invoices;
    @Autowired PaymentService payments;
    @Autowired ServiceItemService items;
    @Autowired ServicePackageService packages;
    @Autowired MechanicService mechanics;

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void readOnlyCanReadButNotWrite() {
        assertDoesNotThrow(() -> invoices.findAll());
        assertDoesNotThrow(() -> payments.findAll());
        assertDoesNotThrow(() -> items.listAll());
        assertDoesNotThrow(() -> packages.findAll());
        assertDoesNotThrow(() -> mechanics.listAll());

        assertThrows(AccessDeniedException.class, () -> invoices.create(1, null));
        assertThrows(AccessDeniedException.class, () -> invoices.markPaid(1));
        assertThrows(AccessDeniedException.class, () -> payments.processPayment(1, "CASH"));
        assertThrows(AccessDeniedException.class, () -> packages.delete(1));
    }

    @Test
    @WithMockUser(roles = "WORKSHOP")
    void workshopCannotInvoiceTakePaymentOrEditCatalog() {
        assertThrows(AccessDeniedException.class, () -> invoices.create(1, null));
        assertThrows(AccessDeniedException.class, () -> invoices.markPaid(1));
        assertThrows(AccessDeniedException.class, () -> payments.processPayment(1, "CASH"));
        assertThrows(AccessDeniedException.class, () -> packages.delete(1));
    }

    @Test
    @WithAdmin
    void adminPassesTheSecurityCheck() {
        assertThrows(EntityNotFoundException.class, () -> invoices.markPaid(999));
    }
}