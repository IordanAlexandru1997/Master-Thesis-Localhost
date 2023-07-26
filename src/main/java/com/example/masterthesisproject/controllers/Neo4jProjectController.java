package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.*;
import com.example.masterthesisproject.services.Neo4jService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.neo4j.driver.types.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class Neo4jProjectController {

    @Autowired
    private Neo4jService neo4jService;


    @PostMapping("/sobo/{uniqueKey}")
    public ResponseEntity<String> addSoBO(@PathVariable String uniqueKey, @RequestBody SoBO soboObj) {
        neo4jService.addSoBO(soboObj, uniqueKey);
        return new ResponseEntity<>("SoBO object added", HttpStatus.CREATED);
    }

    @PostMapping("/create-edge/{uniqueKey}")
    public ResponseEntity<String> createEdge(@PathVariable String uniqueKey, @RequestBody Edge edge) {
        try {
            neo4jService.createEdge(edge, uniqueKey);
            return ResponseEntity.ok("Relationship created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create relationship: " + e.getMessage());
        }
    }


}
