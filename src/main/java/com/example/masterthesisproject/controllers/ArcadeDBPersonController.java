package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Person;
import com.example.masterthesisproject.services.ArcadeDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

@RestController
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('arcadedb')}")

public class ArcadeDBPersonController {

    @Autowired
    private ArcadeDBService arcadeDBService;

    @PostMapping("/arcadeperson")
    public ResponseEntity<String> createPerson(@RequestBody Person person) {
        arcadeDBService.insertPerson(person);
        return new ResponseEntity<>("Person created", HttpStatus.CREATED);
    }

    @GetMapping("/arcadeperson/{name}")
    public ResponseEntity<String> getPersonsByName(@PathVariable String name) {
        String persons = arcadeDBService.getPersonsByName(name);
        return new ResponseEntity<>(persons, HttpStatus.OK);
    }
}
