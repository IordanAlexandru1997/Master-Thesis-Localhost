package com.example.masterthesisproject.services;

import com.example.masterthesisproject.DatabaseBenchmark;
import com.example.masterthesisproject.DatabaseService;
import com.example.masterthesisproject.SoBOGenerator;
import com.example.masterthesisproject.SoBOIdTracker;
import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.Record;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

@Service
@Lazy

public class Neo4jService implements DatabaseService {

    @org.springframework.beans.factory.annotation.Value("${neo4j.url}")
    private String NEO4J_URL;

    @org.springframework.beans.factory.annotation.Value("${neo4j.username}")
    private String USERNAME;

    @org.springframework.beans.factory.annotation.Value("${neo4j.password}")
    private String PASSWORD;

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
    public void clearDatabase() {
        try (Session session = driver.session()) {
            String query = "MATCH (n) DETACH DELETE n";
            session.run(query);
        }
    }

    private static int soboCounter = 0;

    @Override
    public void create(int minEdgesPerNode, int maxEdgesPerNode) {

        SoBO sobo = SoBOGenerator.generateRandomSoBO();
        addSoBO(sobo, "id");

        soboCounter++;
        if (soboCounter >= 2) {
            Edge edge = SoBOGenerator.generateRandomEdge();
            createEdge(edge, "id");
            soboCounter = 0;
        }
        SoBOIdTracker.appendSoBOId(sobo.getId());
    }
    @Override
    public void read() {
        try (Session session = driver.session()) {
            // Load the custom IDs from sobo_obj.json
            List<String> soboIds = SoBOIdTracker.loadSoBOIds();

            if (soboIds.isEmpty()) {
                System.err.println("No SoBOs have been generated.");
                return;
            }

            // Pick a random custom ID
            String randomSoBOId = soboIds.get(new Random().nextInt(soboIds.size()));

            // Use the picked custom ID to fetch the node
            String nodeQuery = "MATCH (s {id: $id}) RETURN s";
            Result nodeResult = session.run(nodeQuery, parameters("id", randomSoBOId));

            if (nodeResult.hasNext()) {
                Node sobo = nodeResult.next().get("s").asNode();
                System.out.println("Selected SoBO with ID: " + sobo.id());  // This will display the default Neo4j node ID

                // Fetch the neighbors of the selected SoBO node considering all possible relationships
                String neighborsQuery = "MATCH (s)-[r:RELATED_TO|FRIENDS_WITH|WORKS_WITH]->(neighbor) WHERE ID(s) = $id RETURN neighbor, type(r) as relationshipType";
                Result neighborsResult = session.run(neighborsQuery, parameters("id", sobo.id()));

                StringBuilder neighbors = new StringBuilder("Related Neighbors: \n");
                while (neighborsResult.hasNext()) {
                    Record record = neighborsResult.next();
                    Node neighbor = record.get("neighbor").asNode();
                    String relationshipType = record.get("relationshipType").asString();
                    neighbors.append("Neighbor ID: ").append(neighbor.id()).append(", Relationship: ").append(relationshipType).append("\n");
                }

                System.out.println(neighbors.toString());

            } else {
                System.err.println("No SoBO found for custom ID: " + randomSoBOId);
            }
        }
    }



    private final List<String> updatedIds = new ArrayList<>();

    @Override
    public void update() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds(); // Load SoBO IDs

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform update operation.");
            return;
        }

        soboIds.removeAll(updatedIds); // Remove already updated IDs

        if (soboIds.isEmpty()) {
            System.out.println("All SoBOs have been updated.");
            return;
        }

        String id = getRandomSoBOId(soboIds); // Select a random ID from the remaining IDs
        System.out.println("Selected ID for update: " + id);

        try (Session session = driver.session()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("MATCH (s {");
            queryBuilder.append("`id`").append(": $").append("id");
            queryBuilder.append("}) SET s.name = 'Updated Field'");

            session.run(queryBuilder.toString(), parameters("id", id));

            updatedIds.add(id); // Add to updated IDs
            System.out.println("Updated ID: " + id);
        }
    }

    @Override
    public void delete() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform delete operation.");
            return;
        }

        String soboIdToDelete = getRandomSoBOId(soboIds); // Pick from the loaded IDs
        System.out.println("Selected SoBO ID for deletion: " + soboIdToDelete);

        try (Session session = driver.session()) {
            String query = "MATCH (s {id: $soboId}) DETACH DELETE s";
            session.run(query, parameters("soboId", soboIdToDelete));
        }

        soboIds.remove(soboIdToDelete);
        SoBOIdTracker.saveSoBOIds(soboIds);
    }



    private String getRandomSoBOId() {
        return SoBOGenerator.getRandomSoBOId();
    }
    private String getRandomSoBOId(List<String> soboIds) {
        int randomIndex = new Random().nextInt(soboIds.size());
        return soboIds.get(randomIndex);
    }

    @Override
    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete, int numEntries, int minEdgesPerNode, int maxEdgesPerNode) {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(this, numEntries);
        benchmark.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, minEdgesPerNode, maxEdgesPerNode);
    }

}

