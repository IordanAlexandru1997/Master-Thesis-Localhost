package com.example.masterthesisproject.entities;

public class Employee {
    private String name;
    private int age;
    private double salary;
    private String id;
    private String department;
    private String position;

    public Employee(){

    }
    public Employee(String name, int age, double salary, String id, String department, String position) {
        this.name = name;
        this.salary = salary;
        this.id = id;
        this.department = department;
    }

    public Employee(String employeeName, int age) {
        this.name = employeeName;
        this.age = age;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
}
