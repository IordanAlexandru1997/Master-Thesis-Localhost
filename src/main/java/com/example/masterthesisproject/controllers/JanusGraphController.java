package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Person;
import com.example.masterthesisproject.services.JanusGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class JanusGraphController {

    @Autowired
    private JanusGraphService janusGraphService;

    @PostMapping("/janusgraphperson")
    public ResponseEntity<String> createPerson(@RequestBody Person person) {
        janusGraphService.insertPerson(person);
        return new ResponseEntity<>("Person created", HttpStatus.CREATED);
    }

    @GetMapping("/janusgraphperson/{name}")
    public ResponseEntity<Person> getPersonsByName(@PathVariable String name) {
        Person person = janusGraphService.getPersonsByName(name);
        return new ResponseEntity<>(person, HttpStatus.OK);
    }
}