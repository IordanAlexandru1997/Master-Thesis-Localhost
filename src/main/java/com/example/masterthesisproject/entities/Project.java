package com.example.masterthesisproject.entities;

public class Project {
    private String name;
    private String id;
    private Employee employee;
    private Invoice invoice;
    public Project(){}
    public Project(String name, String id, Employee employee, Invoice invoice) {
        this.name = name;
        this.id = id;
        this.employee = employee;
        this.invoice = invoice;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getId() {
        return this.name; // assuming `name` is a class property
    }
    public void setId(String id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
}
