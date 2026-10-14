package com.wac.autocore.model;

import com.wac.autocore.data.MoneyConverter;
import com.wac.autocore.model.value.Money;

import javax.persistence.*;

@Entity
@Table(name = "invoice_line")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Convert(converter = MoneyConverter.class)
    private Money amount = Money.ZERO;

    @Convert(converter = MoneyConverter.class)
    private Money discount = Money.ZERO;

    @Convert(converter = MoneyConverter.class)
    private Money total = Money.ZERO;

    private String serviceItemName;

    protected InvoiceLine() {}

    public InvoiceLine(Invoice invoice, String serviceItemName, Money amount, Money discount, Money total) {
        this.invoice = invoice;
        this.serviceItemName = serviceItemName;
        this.amount = amount;
        this.discount = discount;
        this.total = total;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    // Kolumnerna i invoice_line är nullable, så en saknad rad i databasen läses som 0
    public Money getAmount() {
        return amount == null ? Money.ZERO : amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public Money getDiscount() {
        return discount == null ? Money.ZERO : discount;
    }

    public void setDiscount(Money discount) {
        this.discount = discount;
    }

    public Money getTotal() {
        return total == null ? Money.ZERO : total;
    }

    public void setTotal(Money total) {
        this.total = total;
    }
}