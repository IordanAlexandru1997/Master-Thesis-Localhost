package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Employee;
import com.example.masterthesisproject.entities.Invoice;
import com.example.masterthesisproject.entities.Project;
import com.example.masterthesisproject.services.DGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('dgraph')}")

public class DGraphProjectController {

    @Autowired
    private DGraphService dGraphService;

    @PostMapping("/dgraph/employee")
    public ResponseEntity<String> createEmployee(@RequestBody Employee employee) {
        dGraphService.createEmployee(employee);
        return new ResponseEntity<>("Employee created", HttpStatus.CREATED);
    }

    @PostMapping("/dgraph/project")
    public ResponseEntity<String> createProject(@RequestBody Project project) {
        dGraphService.createProject(project);
        return new ResponseEntity<>("Project created", HttpStatus.CREATED);
    }

    @PostMapping("/dgraph/invoice")
    public ResponseEntity<String> createInvoice(@RequestBody Invoice invoice) {
        dGraphService.createInvoice(invoice);
        return new ResponseEntity<>("Invoice created", HttpStatus.CREATED);
    }


    @GetMapping("/dgraph/employee/{employeeName}")
    public ResponseEntity<Employee> getEmployeeByName(@PathVariable String employeeName) {
        Employee employee = dGraphService.getEmployeeByName(employeeName);
        if(employee != null) {
            return new ResponseEntity<>(employee, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/dgraph/employee/{employeeName}/project/{projectName}")
    public ResponseEntity<String> createEmployeeProjectRelationship(@PathVariable String employeeName, @PathVariable String projectName) {
        dGraphService.createEmployeeProjectRelationship(employeeName, projectName);
        return new ResponseEntity<>("Employee-Project relationship created", HttpStatus.CREATED);
    }

    @PostMapping("/dgraph/employee/{employeeName}/invoice/{invoiceId}")
    public ResponseEntity<String> createEmployeeInvoiceRelationship(@PathVariable String employeeName, @PathVariable String invoiceId) {
        dGraphService.createEmployeeInvoiceRelationship(employeeName, invoiceId);
        return new ResponseEntity<>("Employee-Invoice relationship created", HttpStatus.CREATED);
    }

}
