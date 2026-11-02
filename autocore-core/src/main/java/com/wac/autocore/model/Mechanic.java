package com.wac.autocore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mechanic")
public class Mechanic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String phone;
    private String specialization;

    protected Mechanic() {
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

    /* Returns a technical representation for logs and debugging.
     * Contains only the id; name and phone are personal data and must not reach log files */
    @Override
    public String toString() {
        return "Mechanic{id=" + id + "}";
    }
}
