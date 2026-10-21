
package com.wac.autocore.unit;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    @DisplayName("successful creates a payment marked as successful")
    void successfulCreatesSuccessfulPayment() {
        Payment payment = Payment.successful(1, Money.of("100.00"), "CARD");

        assertTrue(payment.isSuccessful());
        assertEquals(1, payment.getInvoiceId());
        assertEquals(Money.of("100.00"), payment.getAmount());
        assertEquals("CARD", payment.getPaymentType());
        assertNotNull(payment.getPaymentDate());
    }

    @Test
    @DisplayName("markinPaid an already paid invoice throws")
    void markinPaidAlreadyPaidInvoiceThrows() {
        Invoice invoice = new Invoice(1, LocalDate.now());
        invoice.markPaid();

        assertThrows(ValidationException.class, invoice::markPaid);
        assertTrue(invoice.isPaid());
    }
}