package com.wac.autocore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean enabled = true;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String role, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /* Returns a technical representation for logs and debugging.
     * The username identifies a person and is left out. Role and enabled are kept
     * because they are useful when debugging access problems */
    @Override
    public String toString() {
        return "AppUser{id=" + id + ", role=" + role + ", enabled=" + enabled + "}";
    }
}
