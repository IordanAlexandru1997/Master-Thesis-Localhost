package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Employee;
import com.example.masterthesisproject.entities.Invoice;
import com.example.masterthesisproject.entities.Project;
import com.example.masterthesisproject.services.Neo4jService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.neo4j.driver.types.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class Neo4jProjectController {

    @Autowired
    private Neo4jService neo4jService;

    @PostMapping("/employee/{employeeName}/project/{projectName}/invoice/{invoiceAmount}")
    public ResponseEntity<String> createRelationships(@PathVariable String employeeName,
                                                      @PathVariable String projectName,
                                                      @PathVariable double invoiceAmount) {
        neo4jService.createRelationships(employeeName, projectName, invoiceAmount);
        return new ResponseEntity<>("Relationships created", HttpStatus.CREATED);
    }
    @PostMapping("/employee/{employeeName}/project/{projectName}")
    public ResponseEntity<String> createRelationshipsWithoutInvoice(@PathVariable String employeeName,
                                                      @PathVariable String projectName) {
        neo4jService.createRelationshipWithoutInvoice(employeeName, projectName);
        return new ResponseEntity<>("Relationships created", HttpStatus.CREATED);
    }


    @GetMapping("/employee/list/{name}")
    public ResponseEntity<List<Employee>> getEmployeesByName(@PathVariable String name) {
        List<Node> employeeNodes = neo4jService.getEmployeesByName(name);
        List<Employee> employeeList = employeeNodes.stream()
                .map(node -> {
                    String employeeName = node.get("name").asString();
                    int age = node.get("age").asInt();
                    return new Employee(employeeName, age);  // Please adjust according to your Employee class constructor
                })
                .collect(Collectors.toList());
        return new ResponseEntity<>(employeeList, HttpStatus.OK);
    }


    @PostMapping("/employee")
    public ResponseEntity<String> createEmployee(@RequestBody Employee employee) {
        neo4jService.createEmployee(employee.getName(), employee.getSalary(), employee.getId(), employee.getDepartment());
        return new ResponseEntity<>("Employee created", HttpStatus.CREATED);
    }

    @GetMapping("/employee/single/{name}")
    public ResponseEntity<List<Map<String, Object>>> getEmployee(@PathVariable String name) {
        List<Map<String, Object>> employees = neo4jService.getEmployee(name);
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }

    @PostMapping("/invoice")
    public ResponseEntity<String> createInvoice(@RequestBody Invoice invoice) {
        neo4jService.createInvoice(invoice.getId(), invoice.getCustomer(), invoice.getAmount());
        return new ResponseEntity<>("Invoice created", HttpStatus.CREATED);
    }

    @GetMapping("/invoice/{id}")
    public ResponseEntity<List<Map<String, Object>>> getInvoice(@PathVariable String id) {
        List<Map<String, Object>> invoices = neo4jService.getInvoice(id);
        return new ResponseEntity<>(invoices, HttpStatus.OK);
    }

    @PostMapping("/project")
    public ResponseEntity<String> createProject(@RequestBody Project project) {
        neo4jService.createProject(project.getId(), project.getName());
        return new ResponseEntity<>("Project created", HttpStatus.CREATED);
    }

    @GetMapping("/project/{id}")
    public ResponseEntity<List<Map<String, Object>>> getProject(@PathVariable String id) {
        List<Map<String, Object>> projects = neo4jService.getProject(id);
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }
}
