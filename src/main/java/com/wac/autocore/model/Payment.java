package com.wac.autocore.model;

import com.wac.autocore.data.MoneyConverter;
import com.wac.autocore.model.value.Money;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int invoiceId;
    @Convert(converter = MoneyConverter.class)
    private Money amount;
    private String paymentType;
    private LocalDateTime paymentDate;

    @Type(type = "org.hibernate.type.NumericBooleanType")
    private boolean successful = false;

    protected Payment() {}

    public static Payment successful(int invoiceId, Money amount, String paymentType) {
        Payment payment = new Payment();
        payment.invoiceId = invoiceId;
        payment.amount = amount;
        payment.paymentType = paymentType;
        payment.paymentDate = LocalDateTime.now();
        payment.successful = true;
        return payment;
    }

    public int getId() {
        return id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }


    public Money getAmount() {
        return amount;
    }


    public String getPaymentType() {
        return paymentType;
    }


    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }


    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public String toString() {
        return id +
                " - Invoice ID: " + invoiceId +
                " | Amount: " + amount + " SEK" +
                " | Payment type: " + paymentType +
                " | Date: " + paymentDate +
                " | Successful: " + (successful ? "Yes" : "No");
    }
}