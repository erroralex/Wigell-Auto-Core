package com.wac.autocore.model;

import javax.persistence.*;

@Entity
@Table(name = "invoice_line")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    private String serviceItemName;
    private double amount;
    private double discount;


    protected InvoiceLine() {}

    public InvoiceLine(Invoice invoice, String serviceItemName, double amount, double discount) {
        this.invoice = invoice;
        this.serviceItemName = serviceItemName;
        this.amount = amount;
        this.discount = discount;
    }

    public double getTotalAmount() {
        return Math.max(amount - discount, 0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getServiceItemName() {
        return serviceItemName;
    }

    public void setServiceItemName(String serviceItemName) {
        this.serviceItemName = serviceItemName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }
}