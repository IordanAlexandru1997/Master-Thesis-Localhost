package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Person;
import com.example.masterthesisproject.services.DGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('dgraph')}")

public class DGraphPersonController {

    @Autowired
    private DGraphService dGraphService;

    @PostMapping("/dgraphperson")
    public ResponseEntity<String> createPerson(@RequestBody Person person) {
        dGraphService.insertPerson(person);
        return new ResponseEntity<>("Person created", HttpStatus.CREATED);
    }

    @GetMapping("/dgraphperson/{name}")
    public ResponseEntity<String> getPersonsByName(@PathVariable String name) {
        String persons = dGraphService.getPersonsByName(name);
        return new ResponseEntity<>(persons, HttpStatus.OK);
    }
}
