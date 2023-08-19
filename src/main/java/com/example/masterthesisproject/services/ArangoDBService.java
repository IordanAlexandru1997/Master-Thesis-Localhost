package com.example.masterthesisproject.services;
import com.arangodb.*;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.entity.CollectionType;

import com.arangodb.entity.BaseDocument;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.util.MapBuilder;
import com.example.masterthesisproject.DatabaseBenchmark;
import com.example.masterthesisproject.DatabaseService;
import com.example.masterthesisproject.SoBOGenerator;
import com.example.masterthesisproject.SoBOIdTracker;
import com.example.masterthesisproject.entities.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.io.FileWriter;
import java.io.IOException;
import javax.json.Json;



import javax.annotation.PostConstruct;
import javax.json.JsonObjectBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBO_IDs;
import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBOs;

@Service
@Lazy

public class ArangoDBService implements DatabaseService {

    @Value("${arangodb.host}")
    private String ARANGO_DB_HOST;

    @Value("${arangodb.port}")
    private int ARANGO_DB_PORT;

    @Value("${arangodb.user}")
    private String ARANGO_DB_USER;

    @Value("${arangodb.password}")
    private String ARANGO_DB_PASSWORD;

    @Value("${arangodb.dbname}")
    private String DB_NAME;
    @Value("${optimization.enabled}")
    private boolean optimizationEnabled;
    @Value("${arangodb.collection}")
    private String COLLECTION_NAME;
    private ArangoDB arangoDB;
    private ArangoDatabase database;
    private Boolean uiOptimizationFlag = null;
    private static final String OPERATIONAL_LOG_FILE = "operational_logs.json";

    public boolean isOptimizationEffective() {
        return uiOptimizationFlag != null ? uiOptimizationFlag : optimizationEnabled;
    }
    public void setUiOptimizationFlag(boolean flag) {
        this.uiOptimizationFlag = flag;
    }
    @PostConstruct
    public void init() {
        arangoDB = new ArangoDB.Builder()
                .host(ARANGO_DB_HOST, ARANGO_DB_PORT)
                .user(ARANGO_DB_USER)
                .password(ARANGO_DB_PASSWORD)
                .build();
        database = arangoDB.db(DB_NAME);

        // Ensure SoBO collection exists
        ArangoCollection soboCollection = database.collection("SoBO");
        if (!soboCollection.exists()) {
            database.createCollection("SoBO");
        }

        // Ensure edgeCollection exists
        if (!database.collection("edgeCollection").exists()) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.type(CollectionType.EDGES);
            database.createCollection("edgeCollection", options);
        }

        // Ensure sobo_graph exists
        ArangoGraph graph = database.graph("sobo_graph");
        if (!graph.exists()) {
            GraphCreateOptions graphOptions = new GraphCreateOptions();
            graph.create(Collections.singletonList(new EdgeDefinition()
                    .collection("edgeCollection")
                    .from("SoBO")
                    .to("SoBO")), graphOptions);
        }
        if (isOptimizationEffective()) {
            // Create index on 'id' attribute
            soboCollection.ensureHashIndex(List.of("id"), new HashIndexOptions());
        }
    }


    private void logOperation(String operation, String message) {
        try (FileWriter file = new FileWriter(OPERATIONAL_LOG_FILE, true)) {
            JsonObjectBuilder logObjectBuilder = Json.createObjectBuilder();
            logObjectBuilder.add("database", "ArangoDB")
                    .add("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                    .add("details", Json.createObjectBuilder()
                            .add("operation", operation)
                            .add("message", message));
            file.write(logObjectBuilder.build().toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSoBO(SoBO sobo, String keyAttr) {
        // Define a document
        BaseDocument soboDoc = new BaseDocument(sobo.getId());
        soboDoc.setProperties(sobo.getProperties());

        // Check if document exists and update or insert accordingly
        if (database.collection("SoBO").documentExists(sobo.getId())) {
            database.collection("SoBO").updateDocument(sobo.getId(), soboDoc);
        } else {
            database.collection("SoBO").insertDocument(soboDoc);
        }
        logOperation("Create", "Added a new SoBO with ID: " + sobo.getId());


    }
    public void createEdge(Edge edge, String edgeCollectionName) {
        // Check if Edge collection exists and create it if not
        if (!database.collection(edgeCollectionName).exists()) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.type(CollectionType.EDGES);
            database.createCollection(edgeCollectionName, options);
        }

        String id1 = (String) edge.getSoboObj1().getProperties().get("id");
        String id2 = (String) edge.getSoboObj2().getProperties().get("id");

        // Check if an edge already exists between the two SoBOs
        String query = "FOR edge IN @@collectionName FILTER edge._from == @sourceId AND edge._to == @targetId RETURN edge";
        Map<String, Object> bindVars = Map.of("@collectionName", edgeCollectionName, "sourceId", "SoBO/" + id1, "targetId", "SoBO/" + id2);
        ArangoCursor<BaseEdgeDocument> cursor = database.query(query, bindVars, null, BaseEdgeDocument.class);

        if (cursor.hasNext()) {
            // Edge already exists, return without creating a new edge
            return;
        }

        // Create Edge document
        String edgeKey = UUID.randomUUID().toString();
        Map<String, Object> properties = edge.getProperties();
        if(properties == null) {
            properties = new HashMap<>();
        }
        BaseEdgeDocument edgeDoc = new BaseEdgeDocument("SoBO/" + id1, "SoBO/" + id2);
        edgeDoc.setKey(edgeKey);
        edgeDoc.setProperties(properties);

        BaseEdgeDocument existingEdge = database.collection(edgeCollectionName).getDocument(edgeKey, BaseEdgeDocument.class);
        if (existingEdge != null) {
            database.collection(edgeCollectionName).updateDocument(edgeKey, edgeDoc);
        } else {
            database.collection(edgeCollectionName).insertDocument(edgeDoc);
        }

        // Throw an exception if the edge could not be created
        if (database.collection(edgeCollectionName).getDocument(edgeKey, BaseEdgeDocument.class) == null) {
            throw new RuntimeException("Could not create Edge document with key: " + edgeKey);
        }
        logOperation("Create", "Created a new Edge with key: " + edgeKey);
    }

    @Override
    public void clearDatabase() {
        if (arangoDB.db(DB_NAME).exists()) {
            arangoDB.db(DB_NAME).drop();
        }
        arangoDB.createDatabase(DB_NAME);
        arangoDB.db(DB_NAME).createCollection(COLLECTION_NAME);
        init();
    }

    @Override
    public void create(int minEdgesPerNode, int maxEdgesPerNode) {
        // Create and add SoBO

        SoBO sobo = SoBOGenerator.generateRandomSoBO();
        addSoBO(sobo, "id");
        GENERATED_SoBOs.add(sobo);
        GENERATED_SoBO_IDs.add(sobo.getId());
        SoBOIdTracker.appendSoBOId(sobo.getId());

        // Determine how many edges to generate for this SoBO
        int numEdges = new Random().nextInt(maxEdgesPerNode - minEdgesPerNode + 1) + minEdgesPerNode;

        for (int i = 0; i < numEdges; i++) {
            // Randomly select a previous SoBO to connect with
            SoBO targetSoBO = GENERATED_SoBOs.get(new Random().nextInt(GENERATED_SoBOs.size() - 1));

            // Avoid self-connections
            if (!sobo.equals(targetSoBO)) {
                Edge edge = new Edge(sobo, targetSoBO, "RELATED_TO");
                createEdge(edge, "edgeCollection");
            }
        }
    }
    @Override
    public void read() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();
        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated.");
            return;
        }
        String randomSoBOId = soboIds.get(new Random().nextInt(soboIds.size()));

        StringBuilder neighbors = new StringBuilder();

        if (isOptimizationEffective()) {
            String neighborsQuery = "FOR neighbor, edge IN 1..1 OUTBOUND @startVertex GRAPH @graphName " +
                    "RETURN {neighbor: neighbor, relationshipType: edge.type}";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("startVertex", "SoBO/" + randomSoBOId);
            bindVars.put("graphName", "sobo_graph");
            ArangoCursor<HashMap> neighborsResult = database.query(neighborsQuery, bindVars, null, HashMap.class);
            while (neighborsResult.hasNext()) {
                HashMap<String, Object> record = neighborsResult.next();
                if (record != null && record.containsKey("neighbor")) {
                    HashMap<String, Object> neighborMap = (HashMap<String, Object>) record.get("neighbor");
                    if (neighborMap != null) {
                        String relationshipType = (String) record.get("relationshipType");
                        String croppedNeighborId = neighborMap.get("_id").toString().replace("SoBO/", "");
                        neighbors.append("Neighbor ID: ").append(croppedNeighborId).append(", Relationship: ").append(relationshipType).append("\n");
                    }
                }
            }
        }  else {
            String nodeQuery = "FOR s IN SoBO FILTER s.id == @id RETURN s";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("id", randomSoBOId);
            ArangoCursor<BaseDocument> nodeResult = database.query(nodeQuery, bindVars, null, BaseDocument.class);
            if (nodeResult.hasNext()) {
                BaseDocument sobo = nodeResult.next();
                String neighborsQuery = "FOR neighbor, edge IN OUTBOUND @id edgeCollection RETURN {neighbor: neighbor, relationshipType: edge.type}";
                bindVars = new MapBuilder().put("id", sobo.getId()).get();
                ArangoCursor<HashMap> neighborsResult = database.query(neighborsQuery, bindVars, null, HashMap.class);
                while (neighborsResult.hasNext()) {
                    HashMap<String, Object> record = neighborsResult.next();
                    if (record != null && record.containsKey("neighbor")) {
                        HashMap<String, Object> neighborMap = (HashMap<String, Object>) record.get("neighbor");
                        if (neighborMap != null) {
                            String relationshipType = (String) record.get("relationshipType");
                            String croppedNeighborId = neighborMap.get("_id").toString().replace("SoBO/", "");
                            neighbors.append("Neighbor ID: ").append(croppedNeighborId).append(", Relationship: ").append(relationshipType).append("\n");
                        }
                    }
                }
            } else {
                System.err.println("No SoBO found for custom ID: " + randomSoBOId);
            }
        }

        logOperation("Read", "Selected SoBO with ID: " + randomSoBOId + "; Neighbors: " + neighbors.toString());

    }

    private final List<String> updatedIds = new ArrayList<>();
    public String getRandomSoBOId(List<String> soboIds) {
        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot fetch a random SoBO ID.");
            return null; // or throw an exception, depending on your use case
        }
        int randomIndex = new Random().nextInt(soboIds.size());
        return soboIds.get(randomIndex);
    }
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

        try {
            if (database.collection("SoBO").documentExists(id)) {
                // Retrieve the existing document
                BaseDocument document = database.collection("SoBO").getDocument(id, BaseDocument.class);

                // Update the 'name' field directly
                document.addAttribute("age", 99);

                // Update the document in the database
                database.collection("SoBO").updateDocument(id, document);

                updatedIds.add(id); // Add to updated IDs
//                System.out.println("Updated ID: " + id);
            } else {
                System.err.println("Document not found for ID: " + id);
            }
        } catch (ArangoDBException e) {
            throw new RuntimeException("Failed to update Document with key: " + id, e);
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

        if (database.collection("SoBO").documentExists(soboIdToDelete)) {
            try {
                database.collection("SoBO").deleteDocument(soboIdToDelete);
            } catch (ArangoDBException e) {
                throw new RuntimeException("Failed to delete Document with key: " + soboIdToDelete + " from the database.", e);
            }
        } else {
            System.err.println("Document not found for ID: " + soboIdToDelete);
        }

        soboIds.remove(soboIdToDelete);
        SoBOIdTracker.saveSoBOIds(soboIds);
        logOperation("Delete", "Deleted SoBO with ID: " + soboIdToDelete);

    }


    @Override
    public String getDatabaseName() {
        return "ArangoDB";
    }


}

