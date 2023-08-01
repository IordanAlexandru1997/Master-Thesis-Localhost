package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Employee;
import com.example.masterthesisproject.entities.Invoice;
import com.example.masterthesisproject.entities.Project;
import com.example.masterthesisproject.services.ArangoDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ArangoDBProjectController {

    @Autowired
    private ArangoDBService arangoDBService;
//    @PostMapping("/arango/employee/{employeeName}/project/{projectName}")
//    public ResponseEntity<String> createRelationship(@PathVariable("employeeName") String employeeName, @PathVariable("projectName") String projectName) {
//        try {
//            arangoDBService.createRelationships(employeeName, projectName);
//            return new ResponseEntity<>("Relationship created", HttpStatus.CREATED);
//        } catch (RuntimeException e) {
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    @PostMapping("arango/employee/{employeeName}/projectWithoutInvoice/{projectName}")
//    public ResponseEntity<String> createRelationshipsWithoutInvoice(@PathVariable String employeeName,
//                                                                    @PathVariable String projectName) {
//        arangoDBService.createRelationshipWithoutInvoice(employeeName, projectName);
//        return new ResponseEntity<>("Relationships created", HttpStatus.CREATED);
//    }
//
//    @GetMapping("arango/employee/list/{name}")
//    public ResponseEntity<List<Employee>> getEmployeesByName(@PathVariable String name) {
//        List<Employee> employeeList = arangoDBService.getEmployeesByName(name);
//        return new ResponseEntity<>(employeeList, HttpStatus.OK);
//    }
//
//    @PostMapping("arango/employee")
//    public ResponseEntity<String> createEmployee(@RequestBody Employee employee) {
//        arangoDBService.createEmployee(employee);
//        return new ResponseEntity<>("Employee created", HttpStatus.CREATED);
//    }
//
//    @GetMapping("arango/employee/single/{name}")
//    public ResponseEntity<Employee> getEmployee(@PathVariable String name) {
//        Employee employee = arangoDBService.getEmployee(name);
//        return new ResponseEntity<>(employee, HttpStatus.OK);
//    }
//
//    @PostMapping("arango/invoice")
//    public ResponseEntity<String> createInvoice(@RequestBody Invoice invoice) {
//        arangoDBService.createInvoice(invoice);
//        return new ResponseEntity<>("Invoice created", HttpStatus.CREATED);
//    }
//
//    @GetMapping("arango/invoice/{id}")
//    public ResponseEntity<Invoice> getInvoice(@PathVariable String id) {
//        Invoice invoice = arangoDBService.getInvoice(id);
//        return new ResponseEntity<>(invoice, HttpStatus.OK);
//    }
//
//    @PostMapping("arango/project")
//    public ResponseEntity<String> createProject(@RequestBody Project project) {
//        arangoDBService.createProject(project);
//        return new ResponseEntity<>("Project created", HttpStatus.CREATED);
//    }
//
//    @GetMapping("arango/project/{id}")
//    public ResponseEntity<Project> getProject(@PathVariable String id) {
//        Project project = arangoDBService.getProject(id);
//        return new ResponseEntity<>(project, HttpStatus.OK);
//    }
//    @PostMapping("/arango/employee/{employeeName}/invoice")
//    public ResponseEntity<String> createInvoiceAndRelationship(@PathVariable("employeeName") String employeeName, @RequestBody Invoice invoice) {
//        try {
//            arangoDBService.createInvoiceAndRelationship(employeeName, invoice);
//            return new ResponseEntity<>("Invoice created and relationship established", HttpStatus.CREATED);
//        } catch (RuntimeException e) {
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//    }
}
