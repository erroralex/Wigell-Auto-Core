package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.data.MoneyConverter;

import jakarta.persistence.*;

@Entity
@Table(name = "service_item")
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String description;
    @Convert(converter = MoneyConverter.class)
    private Money price;
    private int estimatedMinutes;

    protected ServiceItem() {}

    public ServiceItem(String name, String description,
                       Money price, int estimatedMinutes) {
        this.name = name;
        this.description = description;
        this.price = requireValidPrice(price);
        this.estimatedMinutes = estimatedMinutes;
    }

    private static Money requireValidPrice(Money price) {
        if (price == null || !price.isPositive()) {
            throw new ValidationException("error.serviceItem.invalidPrice");
        }
        return price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Money getPrice() {
        return price;
    }

    // Used to change the price of a ServiceItem, with validation of the new price.
    public void changePrice(Money newPrice) {
        this.price = requireValidPrice(newPrice);
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    @Override
    public String toString() {
        return id + " - " + name +
                " | Price: " + price + " SEK" +
                " | Estimated time: " + estimatedMinutes + " min";
    }
}