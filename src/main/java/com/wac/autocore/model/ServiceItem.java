package com.wac.autocore.model;

import javax.persistence.*;

@Entity
@Table(name = "service_item")
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String description;
    private double price;
    private int estimatedMinutes;

    protected ServiceItem() {}

    public ServiceItem(String name, String description,
                       double price, int estimatedMinutes) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.estimatedMinutes = estimatedMinutes;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
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