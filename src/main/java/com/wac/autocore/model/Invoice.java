package com.wac.autocore.model;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int workOrderId;
    private LocalDate invoiceDate;
    private double amount;
    private double discount = 0.0;
    private double totalAmount;

    @Type(type = "org.hibernate.type.NumericBooleanType")
    private boolean paid = false;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() {}

    public Invoice(int workOrderId, LocalDate invoiceDate) {
        this.workOrderId = workOrderId;
        this.invoiceDate = invoiceDate;
    }

    public int getId() {
        return id;
    }

    public int getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(int workOrderId) {
        this.workOrderId = workOrderId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
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

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public List<InvoiceLine> getLines() {
        return this.lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
    }

    public void addLine(InvoiceLine line) {
        this.lines.add(line);
    }

    @Override
    public String toString() {
        return id +
                " - Work order ID: " + workOrderId +
                " | Date: " + invoiceDate +
                " | Amount: " + amount + " SEK" +
                " | Discount: " + discount + " SEK" +
                " | Total: " + totalAmount + " SEK" +
                " | Paid: " + (paid ? "Yes" : "No");
    }
}