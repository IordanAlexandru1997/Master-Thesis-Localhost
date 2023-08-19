package com.example.masterthesisproject.services;
import com.arangodb.*;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.entity.CollectionType;

import com.arangodb.entity.BaseDocument;
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

import javax.annotation.PostConstruct;
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
        ArangoCollection collection = arangoDB.db(DB_NAME).collection(COLLECTION_NAME);
        if (!collection.exists()) {
            arangoDB.db(DB_NAME).createCollection(COLLECTION_NAME);
        }
        if (isOptimizationEffective()) {
            // Create index on 'id' attribute
            collection.ensureHashIndex(List.of("id"), new HashIndexOptions());
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
    }

    public void createEdge(Edge edge, String edgeCollectionName) {
        // Check if Edge collection exists and create it if not
        if (!database.collection(edgeCollectionName).exists()) {
            CollectionCreateOptions options = new CollectionCreateOptions();
            options.type(CollectionType.EDGES);
            database.createCollection(edgeCollectionName, options);
        }

        // Create Edge document
        String edgeKey = UUID.randomUUID().toString();
        System.out.println("Generated edgeKey: " + edgeKey);  // Log the generated edgeKey

        String id1 = (String) edge.getSoboObj1().getProperties().get("id");
        String id2 = (String) edge.getSoboObj2().getProperties().get("id");

        Map<String, Object> properties = edge.getProperties();
        if(properties == null) {
            properties = new HashMap<>();
        }
        BaseEdgeDocument edgeDoc = new BaseEdgeDocument("SoBO/" + id1, "SoBO/" + id2);
        edgeDoc.setKey(edgeKey);
        edgeDoc.setProperties(properties);

        BaseEdgeDocument existingEdge = database.collection(edgeCollectionName).getDocument(edgeKey, BaseEdgeDocument.class);
        if (existingEdge != null) {
//            System.out.println("Edge with edgeKey already exists, updating: " + edgeKey);
            database.collection(edgeCollectionName).updateDocument(edgeKey, edgeDoc);
        } else {
//            System.out.println("Creating new Edge with edgeKey: " + edgeKey);
            database.collection(edgeCollectionName).insertDocument(edgeDoc);
        }

        // Throw an exception if the edge could not be created
        if (database.collection(edgeCollectionName).getDocument(edgeKey, BaseEdgeDocument.class) == null) {
            throw new RuntimeException("Could not create Edge document with key: " + edgeKey);
        }
    }
//    public void clearDatabase() {
//        if (database.collection("SoBO").exists()) {
//            database.collection("SoBO").truncate();
//        }
//    }

    @Override
    public void clearDatabase() {
        if (arangoDB.db(DB_NAME).exists()) {
            arangoDB.db(DB_NAME).drop();
        }
        arangoDB.createDatabase(DB_NAME);
        arangoDB.db(DB_NAME).createCollection(COLLECTION_NAME);
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
        // Load the custom IDs from sobo_obj.json
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();
        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated.");
            return;
        }
        // Pick a random custom ID
        String randomSoBOId = soboIds.get(new Random().nextInt(soboIds.size()));
        // Use the picked custom ID to fetch the node

        if (isOptimizationEffective()) {
            System.out.println("Reading SoBOs");
            System.out.println("Selected SoBO with ID: SoBO/" + randomSoBOId);

            // Use AQL to perform graph traversal to get vertex and its neighbors
            String query = "FOR vertex IN 1..1 OUTBOUND @startVertex GRAPH @graphName RETURN vertex";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("startVertex", "SoBO/" + randomSoBOId);
            bindVars.put("graphName", "sobo_graph");

            // Execute the query on the ArangoDatabase instance
            ArangoCursor<BaseDocument> cursor = database.query(query, bindVars, null, BaseDocument.class);

            StringBuilder neighbors = new StringBuilder("Related Neighbors: \n");
            cursor.forEachRemaining(document -> {
                if (document != null) {
                    neighbors.append("Neighbor ID: ").append(document.getKey()).append("\n");
                } else {
                    System.err.println("Encountered a null vertex in the result set.");
                }
            });
            System.out.println(neighbors.toString());
        } else {
            String nodeQuery = "FOR s IN SoBO FILTER s.id == @id RETURN s";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("id", randomSoBOId);
            ArangoCursor<BaseDocument> nodeResult = database.query(nodeQuery, bindVars, null, BaseDocument.class);
            if (nodeResult.hasNext()) {
                BaseDocument sobo = nodeResult.next();
                System.out.println("Selected SoBO with ID: " + sobo.getId());
                // Fetch the neighbors of the selected SoBO node considering all possible relationships
                String neighborsQuery = "FOR neighbor, edge IN OUTBOUND @id edgeCollection RETURN {neighbor: neighbor, relationshipType: edge.type}";
                bindVars = new MapBuilder().put("id", sobo.getId()).get();
                ArangoCursor<HashMap> neighborsResult = database.query(neighborsQuery, bindVars, null, HashMap.class);
                StringBuilder neighbors = new StringBuilder("Related Neighbors: \n");
                while (neighborsResult.hasNext()) {
                    HashMap<String, Object> record = neighborsResult.next();

                    // Check if the record is not null and contains the "neighbor" key
                    if (record != null && record.containsKey("neighbor")) {
                        HashMap<String, Object> neighborMap = (HashMap<String, Object>) record.get("neighbor");

                        // Ensure that the neighborMap is not null before attempting to retrieve values from it
                        if (neighborMap != null) {
                            BaseDocument neighbor = new BaseDocument();
                            neighbor.setKey((String) neighborMap.get("_key"));
                            neighbor.setId((String) neighborMap.get("_id"));
                            neighbor.setProperties((Map<String, Object>) neighborMap.get("properties"));

                            String relationshipType = (String) record.get("relationshipType");

                            // Crop the "SoBO/" prefix from the neighbor ID
                            String croppedNeighborId = neighbor.getId().replace("SoBO/", "");

                            neighbors.append("Neighbor ID: ").append(croppedNeighborId).append(", Relationship: ").append(relationshipType).append("\n");
                        }
                    }
                }

                System.out.println(neighbors.toString());
            } else {
                System.err.println("No SoBO found for custom ID: " + randomSoBOId);
            }
        }
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
            System.out.println("All SoBOs have been updated.");
            return;
        }

        String id = getRandomSoBOId(soboIds); // Select a random ID from the remaining IDs
        System.out.println("Selected ID for update: " + id);

        try {
            if (database.collection("SoBO").documentExists(id)) {
                // Retrieve the existing document
                BaseDocument document = database.collection("SoBO").getDocument(id, BaseDocument.class);

                // Update the 'name' field directly
                document.addAttribute("age", 99);

                // Update the document in the database
                database.collection("SoBO").updateDocument(id, document);

                updatedIds.add(id); // Add to updated IDs
                System.out.println("Updated ID: " + id);
            } else {
                System.err.println("Document not found for ID: " + id);
            }
        } catch (ArangoDBException e) {
            throw new RuntimeException("Failed to update Document with key: " + id, e);
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
    }


    @Override
    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete, int numEntries, int minEdgesPerNode, int maxEdgesPerNode) {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(this, numEntries);
        benchmark.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, minEdgesPerNode, maxEdgesPerNode);
    }

    @Override
    public int countRecords() {
        return 0;
    }


}

