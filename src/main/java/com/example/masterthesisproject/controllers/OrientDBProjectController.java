package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.services.OrientDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrientDBProjectController {

    @Autowired
    private OrientDBService orientDBService;

//    @PostMapping("orient/employee")
//    public ResponseEntity<String> createEmployee(@RequestBody Employee employee) {
//        orientDBService.insertEmployee(employee.getName(), employee.getSalary(), employee.getId(), employee.getDepartment());
//        return new ResponseEntity<>("Employee created", HttpStatus.CREATED);
//    }
//
//    @PostMapping("orient/project")
//    public ResponseEntity<String> createProject(@RequestBody Project project) {
//        orientDBService.insertProject(project.getId(), project.getName());
//        return new ResponseEntity<>("Project created", HttpStatus.CREATED);
//    }
//
//    @PostMapping("orient/invoice")
//    public ResponseEntity<String> createInvoice(@RequestBody Invoice invoice) {
//        orientDBService.insertInvoice(invoice.getId(), invoice.getCustomer(), invoice.getAmount());
//        return new ResponseEntity<>("Invoice created", HttpStatus.CREATED);
//    }
//    @PostMapping("/assignemployee/{employeeId}/to/project/{projectId}")
//    public ResponseEntity<String> assignEmployeeToProject(@PathVariable String employeeId, @PathVariable String projectId) {
//        orientDBService.insertEmployeeToProject(employeeId, projectId);
//        return new ResponseEntity<>("Employee assigned to Project", HttpStatus.OK);
//    }
//
//    @PostMapping("/assignemployee/{employeeId}/to/invoice/{invoiceId}")
//    public ResponseEntity<String> assignEmployeeToInvoice(@PathVariable String employeeId, @PathVariable String invoiceId) {
//        orientDBService.insertEmployeeToInvoice(employeeId, invoiceId);
//        return new ResponseEntity<>("Employee assigned to Invoice", HttpStatus.OK);
//    }
}
