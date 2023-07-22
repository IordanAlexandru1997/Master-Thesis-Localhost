package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Employee;
import com.example.masterthesisproject.entities.Invoice;
import com.example.masterthesisproject.entities.Project;
import com.example.masterthesisproject.services.JanusGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class JanusGraphProjectController {

    @Autowired
    private JanusGraphService janusGraphService;

    @PostMapping("/janusgraph/employee")
    public ResponseEntity<String> createEmployee(@RequestBody Employee employee) {
        janusGraphService.insertEmployee(employee);
        return new ResponseEntity<>("Employee created", HttpStatus.CREATED);
    }

    @PostMapping("/janusgraph/project")
    public ResponseEntity<String> createProject(@RequestBody Project project) {
        janusGraphService.insertProject(project);
        return new ResponseEntity<>("Project created", HttpStatus.CREATED);
    }

    @PostMapping("/janusgraph/invoice")
    public ResponseEntity<String> createInvoice(@RequestBody Invoice invoice) {
        janusGraphService.insertInvoice(invoice);
        return new ResponseEntity<>("Invoice created", HttpStatus.CREATED);
    }

    @PostMapping("/janusgraph/relationship/employee/{employeeId}/invoice/{invoiceId}")
    public ResponseEntity<String> createEmployeeInvoiceRelationship(@PathVariable String employeeId, @PathVariable String invoiceId) {
        janusGraphService.addRelationshipEmployeeInvoice(employeeId, invoiceId);
        return new ResponseEntity<>("Relationship created", HttpStatus.CREATED);
    }

    @PostMapping("/janusgraph/relationship/employee/{employeeId}/project/{projectId}")
    public ResponseEntity<String> createEmployeeProjectRelationship(@PathVariable String employeeId, @PathVariable String projectId) {
        janusGraphService.addRelationshipEmployeeProject(employeeId, projectId);
        return new ResponseEntity<>("Relationship created", HttpStatus.CREATED);
    }

}