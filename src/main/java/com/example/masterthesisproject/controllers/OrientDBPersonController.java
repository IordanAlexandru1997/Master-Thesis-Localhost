package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.services.OrientDBService;
import com.example.masterthesisproject.entities.Person;
import com.orientechnologies.orient.core.record.OElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('orientdb')}")

public class OrientDBPersonController {

    @Autowired
    private OrientDBService orientDBService;

    @PostMapping("/orientperson")
    public ResponseEntity<String> createPerson(@RequestBody Person person) {
        orientDBService.insertPerson(person.getName(), person.getAge());
        return new ResponseEntity<>("Person created", HttpStatus.CREATED);
    }

    @GetMapping("/orientperson/{name}")
    public ResponseEntity<List<Person>> getPersonsByName(@PathVariable String name) {
        List<OElement> persons = orientDBService.getPersonsByName(name);
        List<Person> personList = persons.stream()
                .map(element -> {
                    Person person = new Person();
                    person.setName(element.getProperty("name"));
                    person.setAge(element.getProperty("age"));
                    return person;
                })
                .collect(Collectors.toList());
        return new ResponseEntity<>(personList, HttpStatus.OK);
    }
}