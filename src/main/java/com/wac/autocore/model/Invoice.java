package com.wac.autocore.model;

import com.wac.autocore.data.MoneyConverter;
import com.wac.autocore.model.value.Money;
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

    @Convert(converter = MoneyConverter.class)
    private Money amount = Money.ZERO;

    @Convert(converter = MoneyConverter.class)
    private Money discount = Money.ZERO;

    @Convert(converter = MoneyConverter.class)
    private Money totalAmount = Money.ZERO;

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

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getDiscount() {
        return discount;
    }


    public Money getTotalAmount() {
        return totalAmount;
    }

    public boolean isPaid() {
        return paid;
    }

    public List<InvoiceLine> getLines() {
        return this.lines;
    }


    public void addLine(String serviceItemName, Money amount, Money discount) {
        InvoiceLine line = new InvoiceLine(this, serviceItemName, amount, discount);
        this.lines.add(line);
        this.amount = this.amount.add(line.getAmount());
        this.discount = this.discount.add(line.getDiscount());
        this.totalAmount = this.totalAmount.add(line.getTotal());
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