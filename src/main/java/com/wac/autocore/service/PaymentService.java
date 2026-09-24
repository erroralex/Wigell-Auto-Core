package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.PaymentRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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

    public Payment processPayment(Integer invoiceId, String paymentType) {

        Invoice invoice = invoiceService.findById(invoiceId).get();

        Payment payment = new Payment(invoiceId, invoice.getAmount(), paymentType);
        payment.setSuccessful(true);

        invoice.setPaid(true);
        invoiceService.update(invoice);

        return paymentRepo.save(payment);
    }
}