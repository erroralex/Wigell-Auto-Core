package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.PaymentRepo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Validated
@Service
@Transactional
@PreAuthorize("@access.can('DATA_READ')") // Ensures that only users with 'DATA_READ' permission can access the service methods.
public class PaymentService {

    private final PaymentRepo paymentRepo;
    private final InvoiceService invoiceService;

    public PaymentService(PaymentRepo paymentRepo, InvoiceService invoiceService) {
        this.paymentRepo = paymentRepo;
        this.invoiceService = invoiceService;
    }

    public List<Payment> findAll() {
        return paymentRepo.findAll();
    }

    @PreAuthorize("@access.can('PAYMENT_REGISTER')") // Ensures that only users with 'PAYMENT_REGISTER' permission can register payments.
    public Payment processPayment(@Positive @NotNull(message="error.payment.noInvoiceId") Integer invoiceId,
                                  @NotBlank(message="error.payment.emptyPaymentType")
                                  @Pattern(
                                          regexp = "^(CARD|SWISH|CASH)$",
                                          message = "error.payment.invalidType"
                                  ) String paymentType) {

        Invoice invoice = invoiceService.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", invoiceId));


        Payment payment = Payment.successful(invoiceId, invoice.getTotalAmount(), paymentType);

        invoiceService.markPaid(invoiceId);

        return paymentRepo.save(payment);
    }
}
