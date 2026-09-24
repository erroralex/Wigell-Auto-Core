package com.wac.autocore.model;

import javax.persistence.*;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String registrationNumber;
    private String brand;
    private String model;
    private int year;
    private int customerId;

    protected Vehicle() {}

    @Deprecated // Tillfällig tills dess att repositories ersätter Database
    public Vehicle(int id, String registrationNumber, String brand, String model, int year, int customerId) {
        this(registrationNumber, brand, model, year, customerId);
        this.id = id;
    }

    public Vehicle(String registrationNumber, String brand,
                   String model, int year, int customerId) {
        this.registrationNumber = registrationNumber;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.customerId = customerId;
    }

    public int getId() {
        return id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return id + " - " +
                registrationNumber + " | " +
                brand + " " + model +
                " | Year: " + year +
                " | Customer ID: " + customerId;
    }
}