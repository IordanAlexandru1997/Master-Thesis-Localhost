package com.example.masterthesisproject.services;

import com.example.masterthesisproject.DatabaseService;
import com.example.masterthesisproject.SoBOGenerator;
import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.Record;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.masterthesisproject.SoBOGenerator.*;
import static org.neo4j.driver.Values.parameters;

@Service
public class Neo4jService implements DatabaseService {

    private final String NEO4J_URL = "bolt://localhost:7687";
    private final String USERNAME = "neo4j";
    private final String PASSWORD = "password";
    private Driver driver;

    @PostConstruct
    public void init() {
        driver = GraphDatabase.driver(NEO4J_URL, AuthTokens.basic(USERNAME, PASSWORD));
    }
//    Updates from 24.07.2023 meeting SoBO


    public void addSoBO(SoBO soboObj, String uniqueField) {
        try (Session session = driver.session()) {
            Map<String, Object> properties = soboObj.getProperties();
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("MERGE (s {");
            queryBuilder.append("`").append(uniqueField).append("`").append(": $").append(uniqueField);
            queryBuilder.append("}) SET s += $properties");

            session.run(queryBuilder.toString(), parameters("properties", properties, uniqueField, properties.get(uniqueField)));
        }
    }

    public void createEdge(Edge edge, String uniqueField) {
        try (Session session = driver.session()) {
            SoBO soboObj1 = edge.getSoboObj1();
            SoBO soboObj2 = edge.getSoboObj2();

            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("MATCH (n {").append("`").append(uniqueField).append("`").append(": $").append("value1}), ");
            queryBuilder.append("(m {").append("`").append(uniqueField).append("`").append(": $").append("value2}) ");
            queryBuilder.append("MERGE (n)-[r:").append(edge.getType()).append("]->(m) SET r += $properties");

            Map<String, Object> params = new HashMap<>();
            params.put("value1", soboObj1.getProperties().get(uniqueField));
            params.put("value2", soboObj2.getProperties().get(uniqueField));
            params.put("properties", edge.getProperties());

            session.run(queryBuilder.toString(), params);
        }
    }
    public void updateSoBO(SoBO soboObj, String uniqueField) {
        try (Session session = driver.session()) {
            Map<String, Object> properties = soboObj.getProperties();
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("MATCH (s {");
            queryBuilder.append("`").append(uniqueField).append("`").append(": $").append(uniqueField);
            queryBuilder.append("}) SET s += $properties");

            session.run(queryBuilder.toString(), parameters("properties", properties, uniqueField, properties.get(uniqueField)));
        }
    }

    public void deleteSoBO(String soboId, String uniqueField) {
        try (Session session = driver.session()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("MATCH (s {");
            queryBuilder.append("`").append(uniqueField).append("`").append(": $").append(uniqueField);
            queryBuilder.append("}) DETACH DELETE s");

            session.run(queryBuilder.toString(), parameters(uniqueField, soboId));
        }
    }

    public SoBO getSoBO(String soboId, String uniqueField) {
        try (Session session = driver.session()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("MATCH (s {");
            queryBuilder.append("`").append(uniqueField).append("`").append(": $").append(uniqueField);
            queryBuilder.append("}) RETURN s");

            List<Record> records = session.run(queryBuilder.toString(), parameters(uniqueField, soboId)).list();

            if (!records.isEmpty()) {
                Record record = records.get(0);
                Node node = record.get("s").asNode();
                SoBO sobo = new SoBO();
                sobo.setId(node.get("id").asString());
                Map<String, Object> properties = new HashMap<>();
                for (String key : node.keys()) {
                    properties.put(key, node.get(key).asObject());
                }
                sobo.getProperties().putAll(properties);

                return sobo;
            }

            return null;
        }
    }




    @Override
    public void create() {
        // Use addSoBO method to create a SoBO object twice to ensure at least two SoBO objects
        SoBO sobo1 = generateRandomSoBO();
        addSoBO(sobo1, "id");

        SoBO sobo2 = generateRandomSoBO();
        addSoBO(sobo2, "id");

        // Use createEdge method to create an edge
        Edge edge = generateRandomEdge();
        createEdge(edge, "id");
    }

    @Override
    public void read() {
        // Use getSoBO method to read a SoBO object
        SoBO sobo = getRandomSoBO(); // Assumes you have a method to get a random SoBO object
        getSoBO(sobo.getId(), "id");
    }

    @Override
    public void update() {
        // Use updateSoBO method to update a SoBO object
        SoBO sobo = getRandomSoBO(); // Assumes you have a method to get a random SoBO object
        sobo.addProperty("someProperty", "newValue"); // Update some property of the SoBO object
        updateSoBO(sobo, "id");
    }

    @Override
    public void delete() {
        // Use deleteSoBO method to delete a SoBO object
        SoBO sobo = getRandomSoBO(); // Assumes you have a method to get a random SoBO object
        deleteSoBO(sobo.getId(), "id");
    }
}

