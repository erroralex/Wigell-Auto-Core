package com.wac.autocore.model;

import com.wac.autocore.data.MoneyConverter;
import com.wac.autocore.exception.ValidationException;
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

    InvoiceLine(Invoice invoice, String serviceItemName, Money amount, Money discount) {
        this.invoice = invoice;
        this.serviceItemName = serviceItemName;

        if (amount.isPositive()) {
            throw new ValidationException("error.invoiceNegativeAmount");
        }

        this.amount = amount;
        this.discount = discount.min(amount);
        this.total = this.amount.subtract(this.discount);
    }

    public int getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public String getServiceItemName() {
        return serviceItemName;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getDiscount() {
        return discount;
    }

    public Money getTotal() {
        return this.total;
    }
}