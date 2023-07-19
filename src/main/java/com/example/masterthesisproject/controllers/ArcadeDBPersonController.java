package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Person;
import com.example.masterthesisproject.services.ArcadeDBService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('arcadedb')}")

public class ArcadeDBPersonController {

    private ArcadeDBService arcadeDBService;

    public void ArcadeDBController(ArcadeDBService arcadeDBService) {
        this.arcadeDBService = arcadeDBService;
    }

    @PostMapping("/person")
    public ResponseEntity<Void> insertPerson(@RequestBody Person person) {
        arcadeDBService.insertPerson(person);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/persons")
    public ResponseEntity<List<Person>> getPersons() {
        List<Person> persons = arcadeDBService.getPersons();
        return ResponseEntity.ok(persons);
    }
}
