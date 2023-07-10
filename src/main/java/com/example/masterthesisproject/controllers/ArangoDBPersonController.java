package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.services.ArangoDBService;
import com.example.masterthesisproject.entities.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('arangodb')}")

public class ArangoDBPersonController {

    @Autowired
    private ArangoDBService arangoDBService;

    @PostMapping("/arangoperson")
    public ResponseEntity<String> createPerson(@RequestBody Person person) {
        arangoDBService.insertPerson(person.getName(), person.getAge());
        return new ResponseEntity<>("Person created", HttpStatus.CREATED);
    }

    @GetMapping("/arangoperson/{name}")
    public ResponseEntity<List<Person>> getPersonsByName(@PathVariable String name) {
        List<Person> persons = arangoDBService.getPersonsByName(name);
        return new ResponseEntity<>(persons, HttpStatus.OK);
    }
}
