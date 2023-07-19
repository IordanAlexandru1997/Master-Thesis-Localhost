package com.example.masterthesisproject.entities;

import java.util.Date;

public class Invoice {
    private String id;
    private String customer;
    private double amount;
    public Invoice(){}
    public Invoice(String id, String customer, double amount, Date date) {
        this.id = id;
        this.customer = customer;
        this.amount = amount;
    }

    // Getters and Setters
    // ... rest of the fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

}
