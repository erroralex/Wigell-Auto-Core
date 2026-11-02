package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.logging.LogKeys;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.PaymentRepo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Service
@Transactional
@PreAuthorize("@access.can('DATA_READ')") // Ensures that only users with 'DATA_READ' permission can access the service methods.
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

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

        Payment saved = paymentRepo.save(payment);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(invoice.getWorkOrderId()))) {
            log.info("Registered payment {} for invoice {} ({})", saved.getId(), invoiceId, paymentType);
        }
        return saved;
    }
}
