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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBO_IDs;
import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBOs;
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
    private static final String OPERATIONAL_LOG_FILE = "operational_logs.json";

    @Value("${optimization.enabled}")
    private boolean optimizationEnabled;
    private Driver driver;
    private Boolean uiOptimizationFlag = null;

    public boolean isOptimizationEffective() {
        return uiOptimizationFlag != null ? uiOptimizationFlag : optimizationEnabled;
    }
    public void setUiOptimizationFlag(boolean flag) {
        this.uiOptimizationFlag = flag;
    }
    @PostConstruct
    public void init() {
        driver = GraphDatabase.driver(NEO4J_URL, AuthTokens.basic(USERNAME, PASSWORD));

        try (Session session = driver.session()) {
            if (isOptimizationEffective()) {
                // Create an index on the id property of the SoBO nodes for faster lookup if optimization is enabled.
                String createIndexQuery = "CREATE INDEX sobo_id_index FOR (n:SoBO) ON (n.id)";
                session.run(createIndexQuery);
            }
        }
    }


    private void logOperation(String operation, String message) {
        try (FileWriter file = new FileWriter(OPERATIONAL_LOG_FILE, true)) {
            JsonObjectBuilder logObjectBuilder = Json.createObjectBuilder();
            logObjectBuilder.add("database", "Neo4j")
                    .add("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                    .add("details", Json.createObjectBuilder()
                            .add("operation", operation)
                            .add("message", message));
            file.write(logObjectBuilder.build().toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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


    public void clearDatabase() {
        try (Session session = driver.session()) {
            String query = "MATCH (n) DETACH DELETE n";
            session.run(query);
        }

    }

    public void addSoBO(SoBO soboObj) {
        String uniqueField = "id"; // Assuming 'id' is the unique field you want to use
        addSoBO(soboObj, uniqueField);
    }
    @Override
    public void create(int minEdgesPerNode, int maxEdgesPerNode) {
        try (Session session = driver.session()) {
            SoBO sobo = SoBOGenerator.generateRandomSoBO();
            addSoBO(sobo);
            GENERATED_SoBOs.add(sobo);
            GENERATED_SoBO_IDs.add(sobo.getId());
            SoBOIdTracker.appendSoBOId(sobo.getId());

            int numEdgesToCreate = new Random().nextInt(maxEdgesPerNode - minEdgesPerNode + 1) + minEdgesPerNode;
            int edgesCreated = 0;

            Set<SoBO> alreadyConnected = new HashSet<>(); // To keep track of nodes we've already connected with
            alreadyConnected.add(sobo);  // Ensure we don't create an edge to the same node

            List<SoBO> potentialConnections = new ArrayList<>(GENERATED_SoBOs);
            Collections.shuffle(potentialConnections);

            for (SoBO targetSoBO : potentialConnections) {
                if (edgesCreated >= numEdgesToCreate) break;
                if (alreadyConnected.contains(targetSoBO)) continue;  // Avoid connecting to already connected nodes

                Edge edge = new Edge(sobo, targetSoBO, "RELATED_TO");
                if (!edgeExists(edge)) {
                    createEdge(edge);
                    edgesCreated++;
                    alreadyConnected.add(targetSoBO);  // Mark this node as connected
                    logOperation("Create", "Created edge from SoBO with ID: " + sobo.getId() + " to SoBO with ID: " + targetSoBO.getId());
                }
            }
            logOperation("Create", "Created SoBO with ID: " + sobo.getId());
        }
    }

    public void createEdge(Edge edge) {  // Removed 'uniqueField' argument since it's not used
        try (Session session = driver.session()) {
            String soboObj1Id = (String) edge.getSoboObj1().getProperties().get("id");
            String soboObj2Id = (String) edge.getSoboObj2().getProperties().get("id");

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("MATCH (n {id: $value1}), ");
            queryBuilder.append("(m {id: $value2}) ");
            queryBuilder.append("MERGE (n)-[r:").append(edge.getType()).append("]->(m) SET r += $properties");

            Map<String, Object> edgeParams = new HashMap<>();
            edgeParams.put("value1", soboObj1Id);
            edgeParams.put("value2", soboObj2Id);
            edgeParams.put("properties", edge.getProperties());

            session.run(queryBuilder.toString(), edgeParams);
        }
    }

    public boolean edgeExists(Edge edge) {
        try (Session session = driver.session()) {
            String soboObj1Id = (String) edge.getSoboObj1().getProperties().get("id");
            String soboObj2Id = (String) edge.getSoboObj2().getProperties().get("id");

            String edgeExistsQuery = "MATCH (a {id: $id1})-[r:RELATED_TO]->(b {id: $id2}) RETURN r";
            Map<String, Object> params = Map.of("id1", soboObj1Id, "id2", soboObj2Id);
            Result resultSet = session.run(edgeExistsQuery, params);

            return resultSet.hasNext();
        }
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
//            System.out.println("Selected SoBO ID: " + randomSoBOId);  // Print the selected SoBO ID
            StringBuilder neighbors = new StringBuilder();
            if (isOptimizationEffective()) {
                // Use Neo4j's API to retrieve a node using custom ID and its neighbors
                String query = "MATCH ({id: $id})-[:RELATED_TO]-(neighbors) RETURN neighbors;";
                Map<String, Object> parameters = Collections.singletonMap("id", randomSoBOId);

                Result resultSet = session.run(query, parameters);

                if (!resultSet.hasNext()) {
                    System.err.println("No neighbors found for SoBO ID: " + randomSoBOId);  // Diagnostic message
                }

                while (resultSet.hasNext()) {
                    Record record = resultSet.next();
                    String neighborId = record.get("neighborId").asString();
                    String relationshipType = record.get("relationshipType").asString();
                    neighbors.append("Neighbor ID: ").append(neighborId).append(", Relationship: ").append(relationshipType).append("\n");
                }
            }

            else {
                // Use the picked custom ID to fetch the node
                String nodeQuery = "MATCH (s {id: $id}) RETURN s";
                Result nodeResult = session.run(nodeQuery, parameters("id", randomSoBOId));

                if (nodeResult.hasNext()) {
                    Node sobo = nodeResult.next().get("s").asNode();
//                    System.out.println("Selected SoBO with ID: " + sobo.id());  // This will display the default Neo4j node ID

                    // Fetch the neighbors of the selected SoBO node considering all possible relationships
                    String neighborsQuery = "MATCH (s)-[r:RELATED_TO|FRIENDS_WITH|WORKS_WITH]->(neighbor) WHERE ID(s) = $id RETURN neighbor, type(r) as relationshipType";
                    Result neighborsResult = session.run(neighborsQuery, parameters("id", sobo.id()));

                    neighbors = new StringBuilder("Related Neighbors: \n");
                    while (neighborsResult.hasNext()) {
                        Record record = neighborsResult.next();
                        Node neighbor = record.get("neighbor").asNode();
                        String relationshipType = record.get("relationshipType").asString();
                        neighbors.append("Neighbor ID: ").append(neighbor.id()).append(", Relationship: ").append(relationshipType).append("\n");
                    }

//                    System.out.println(neighbors.toString());
                } else {
                    System.err.println("No SoBO found for custom ID: " + randomSoBOId);
                }

            }        logOperation("Read", "Read SoBO with custom ID: " + randomSoBOId);

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
//            System.out.println("All SoBOs have been updated.");
            return;
        }

        String id = getRandomSoBOId(soboIds); // Select a random ID from the remaining IDs
//        System.out.println("Selected ID for update: " + id);

        try (Session session = driver.session()) {
            StringBuilder queryBuilder = new StringBuilder();

            queryBuilder.append("MATCH (s {");
            queryBuilder.append("`id`").append(": $").append("id");
            queryBuilder.append("}) SET s.name = 'Updated Field'");

            session.run(queryBuilder.toString(), parameters("id", id));

            updatedIds.add(id); // Add to updated IDs
//            System.out.println("Updated ID: " + id);
        }
        logOperation("Update", "Updated SoBO with ID: " + id);

    }

    @Override
    public void delete() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform delete operation.");
            return;
        }

        String soboIdToDelete = getRandomSoBOId(soboIds); // Pick from the loaded IDs
//        System.out.println("Selected SoBO ID for deletion: " + soboIdToDelete);

        try (Session session = driver.session()) {
            String query = "MATCH (s {id: $soboId}) DETACH DELETE s";
            session.run(query, parameters("soboId", soboIdToDelete));
        }

        soboIds.remove(soboIdToDelete);
        SoBOIdTracker.saveSoBOIds(soboIds);
        logOperation("Delete", "Deleted SoBO with ID: " + soboIdToDelete);

    }



    private String getRandomSoBOId(List<String> soboIds) {
        int randomIndex = new Random().nextInt(soboIds.size());
        return soboIds.get(randomIndex);
    }


    @Override
    public String getDatabaseName() {
        return "Neo4j";
    }


}

