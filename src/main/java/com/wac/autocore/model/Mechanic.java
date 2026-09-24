package com.wac.autocore.model;

import javax.persistence.*;

@Entity
@Table(name = "mechanic")
public class Mechanic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String phone;
    private String specialization;
    private boolean available = true;

    protected Mechanic() {}

    @Deprecated // Tillfällig tills dess att repositories ersätter Database
    public Mechanic(int id, String name, String phone, String specialization) {
        this(name, phone, specialization);
        this.id = id;
    }

    public Mechanic(String name, String phone, String specialization) {
        this.name = name;
        this.phone = phone;
        this.specialization = specialization;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return id + " - " + name +
                " | Phone: " + phone +
                " | Specialization: " + specialization +
                " | Available: " + (available ? "Yes" : "No");
    }
}