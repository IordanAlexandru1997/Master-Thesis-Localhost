package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.entities.CreateRelationshipRequest;
import com.example.masterthesisproject.entities.DatabaseUniversalController;
import com.example.masterthesisproject.entities.SoBO;
import com.example.masterthesisproject.services.Neo4jService;
import org.springframework.stereotype.Service;

@Service
public class Neo4jDatabaseController extends DatabaseUniversalController {

    private Neo4jService neo4jService;

    public Neo4jDatabaseController(Neo4jService neo4jService) {
        this.neo4jService = neo4jService;
    }

    @Override
    protected void addSoBOImplementation(SoBO soboObj, String label) {
        neo4jService.addSoBO(soboObj, label);
    }

    @Override
    protected void createEdgeImplementation(CreateRelationshipRequest request) {
        neo4jService.createEdge(request);
    }

    // Implement other methods for Neo4j
    // ...
}
