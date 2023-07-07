package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.services.OrientDBService;
import com.example.masterthesisproject.entities.Person;
import com.orientechnologies.orient.core.record.OElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class OrientDBPersonController {

    @Autowired
    private OrientDBService orientDBService;

    @PostMapping("/orientperson")
    public ResponseEntity<String> createPerson(@RequestBody Person person) {
        orientDBService.insertPerson(person.getName(), person.getAge());
        return new ResponseEntity<>("Person created", HttpStatus.CREATED);
    }

    @GetMapping("/orientperson/{name}")
    public ResponseEntity<List<OElement>> getPersonsByName(@PathVariable String name) {
        List<OElement> persons = orientDBService.getPersonsByName(name);
        return new ResponseEntity<>(persons, HttpStatus.OK);
    }
}