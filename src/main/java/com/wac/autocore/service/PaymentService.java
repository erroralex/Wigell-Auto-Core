package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.PaymentRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.util.List;

@Validated
@Service
@Transactional
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

    public Payment processPayment(@Positive @NotNull(message="error.payment.noInvoiceId") Integer invoiceId,
                                  @NotBlank(message="error.payment.emptyPaymentType")
                                  @Pattern(
                                          regexp = "^(CARD|SWISH|CASH)$",
                                          message = "error.payment.invalidType"
                                  ) String paymentType) {

        Invoice invoice = invoiceService.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", invoiceId));


        Payment payment = Payment.successful(invoiceId, invoice.getTotalAmount(), paymentType);

        invoice.markPaid();
        invoiceService.update(invoice);

        return paymentRepo.save(payment);
    }
}
