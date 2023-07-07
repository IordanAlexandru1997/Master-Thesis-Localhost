package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.Person;
import com.example.masterthesisproject.repositories.PersonRepository;
import com.example.masterthesisproject.services.Neo4jService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class Neo4JPersonController {

    @Autowired
    private Neo4jService neo4jService;

    @Autowired
    private PersonRepository personRepository;


//  url test  http://localhost:8080/person?name=John
    @PostMapping("/person")
    public ResponseEntity<String> createPerson(@RequestBody String name) {
        neo4jService.createPersonWithRelationship(name, "Friend");
        return new ResponseEntity<>("Person created", HttpStatus.CREATED);
}
    @GetMapping("/person/{name}/friends")
    public ResponseEntity<List<Person>> getFriendsOfPerson(@PathVariable String name) {
        List<Person> friends = neo4jService.getFriendsOfPerson(name);
        return new ResponseEntity<>(friends, HttpStatus.OK);
    }

    @GetMapping("/insert")
    public String insertPerson() {

//        personRepository.deleteAll();
        Person person = new Person();
        person.setName("John");
        person.setAge(25);
        personRepository.save(person);
        return "Person inserted";
    }

    @GetMapping("/find")
    public String findPerson() {
        List<Person> persons = personRepository.findByName("John");
        return persons.isEmpty() ? "Person not found" : persons.stream().map(Person::getName).collect(Collectors.joining(", "));
    }
}
