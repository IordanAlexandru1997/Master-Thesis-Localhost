package com.example.masterthesisproject.services;
import com.arangodb.*;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.entity.CollectionType;
import com.example.masterthesisproject.GlobalEdgeCount;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.util.MapBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import javax.annotation.PostConstruct;
import javax.json.JsonObjectBuilder;
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
    private static final Logger logger = LoggerFactory.getLogger(ArangoDBService.class);
    private static final List<String> EDGE_TYPES = Arrays.asList("RELATED_TO", "FRIENDS_WITH", "WORKS_WITH");

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

        for (String edgeType : EDGE_TYPES) {
            if (!database.collection(edgeType).exists()) {
                CollectionCreateOptions options = new CollectionCreateOptions();
                options.type(CollectionType.EDGES);
                database.createCollection(edgeType, options);
            }
        }

        // Ensure sobo_graph exists
        ArangoGraph graph = database.graph("sobo_graph");
        if (!graph.exists()) {
            GraphCreateOptions graphOptions = new GraphCreateOptions();

            List<EdgeDefinition> edgeDefinitions = new ArrayList<>();

            edgeDefinitions.add(new EdgeDefinition()
                    .collection("FRIENDS_WITH")
                    .from("SoBO")
                    .to("SoBO"));

            edgeDefinitions.add(new EdgeDefinition()
                    .collection("RELATED_TO")
                    .from("SoBO")
                    .to("SoBO"));

            edgeDefinitions.add(new EdgeDefinition()
                    .collection("WORKS_WITH")
                    .from("SoBO")
                    .to("SoBO"));

            graph.create(edgeDefinitions, graphOptions);
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


    @Override
    public void clearDatabase() {
        if (arangoDB.db(DB_NAME).exists()) {
            arangoDB.db(DB_NAME).drop();
        }
        arangoDB.createDatabase(DB_NAME);
        for (String edgeType : EDGE_TYPES) {
            if (database.collection(edgeType).exists()) {
                database.collection(edgeType).truncate();
            }
        }

        arangoDB.db(DB_NAME).createCollection(COLLECTION_NAME);
        init();
    }
    public void createEdge(Edge edge, String edgeCollectionName) {
        String edgeType = (String) edge.getProperties().get("edgeType");

        String id1 = (String) edge.getSoboObj1().getProperties().get("id");
        String id2 = (String) edge.getSoboObj2().getProperties().get("id");

        // Check if edge already exists
        ArangoCursor<BaseEdgeDocument> cursor = database.query(
                "FOR edge IN @@edgeCollectionName FILTER edge._from == @fromId AND edge._to == @toId RETURN edge",
                new MapBuilder().put("@edgeCollectionName", edgeCollectionName).put("fromId", "SoBO/" + id1).put("toId", "SoBO/" + id2).get(),
                BaseEdgeDocument.class
        );

        if (cursor.hasNext()) {
            // Edge already exists
            return;
        }

        // Create Edge document
        String edgeKey = UUID.randomUUID().toString();
        Map<String, Object> properties = edge.getProperties();
        properties.put("edgeType", edgeType);

        BaseEdgeDocument edgeDoc = new BaseEdgeDocument("SoBO/" + id1, "SoBO/" + id2);
        edgeDoc.setKey(edgeKey);
        edgeDoc.setProperties(properties);
        database.collection(edgeType).insertDocument(edgeDoc);

        if (database.collection(edgeCollectionName).getDocument(edgeKey, BaseEdgeDocument.class) == null) {
            throw new RuntimeException("Could not create Edge document with key: " + edgeKey);
        }
        logOperation("Create", "Created a new Edge with key: " + edgeKey);
    }


    public long addSoBO(SoBO sobo, String keyAttr) {
        BaseDocument soboDoc = new BaseDocument(sobo.getId());
        soboDoc.setProperties(sobo.getProperties());
        long startInsertionTime = 0;
        long endInsertionTime = 0;

        startInsertionTime = System.currentTimeMillis();
        database.collection("SoBO").insertDocument(soboDoc);
        endInsertionTime = System.currentTimeMillis();

        logOperation("Create", "Added a new SoBO with ID: " + sobo.getId());
        return endInsertionTime - startInsertionTime;


    }

    @Override
    public long create(int minEdgesPerNode, int maxEdgesPerNode) {

        SoBO sobo = SoBOGenerator.generateRandomSoBO();

        long insertionDuration = addSoBO(sobo, "id");

        GENERATED_SoBOs.add(sobo);
        GENERATED_SoBO_IDs.add(sobo.getId());
        SoBOIdTracker.appendSoBOId(sobo.getId());
        GlobalEdgeCount globalEdgeCount = GlobalEdgeCount.getInstance();
        int numEdgesToCreate = globalEdgeCount.getNumEdgesToCreate();

        if (numEdgesToCreate == 0) {
            globalEdgeCount.setNumEdgesToCreate(minEdgesPerNode, maxEdgesPerNode);
            numEdgesToCreate = globalEdgeCount.getNumEdgesToCreate();
        }

        System.out.println("Arango Num edges to create: "+ numEdgesToCreate);
        int edgesCreated = 0;

        Set<SoBO> alreadyConnected = new HashSet<>();
        alreadyConnected.add(sobo);

        List<SoBO> potentialConnections = new ArrayList<>(GENERATED_SoBOs);
        Collections.shuffle(potentialConnections);

        for (SoBO targetSoBO : potentialConnections) {
            if (edgesCreated == numEdgesToCreate) break;
            if (alreadyConnected.contains(targetSoBO)) {
                logger.warn("Skipping edge creation between {} and {} due to already existing connection", sobo.getId(), targetSoBO.getId());
                continue;
            }


            Edge edge = SoBOGenerator.generateRandomEdge(sobo, targetSoBO);
            String edgeType = (String) edge.getProperties().get("edgeType");

            createEdge(edge, edgeType);

            edgesCreated++;

            alreadyConnected.add(targetSoBO);
        }

        return insertionDuration;
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
            for (String edgeType : EDGE_TYPES) {
                String neighborsQuery = "FOR neighbor, edge IN 1..1 ANY @startVertex " + edgeType +
                        " RETURN {neighbor: neighbor}";

                Map<String, Object> bindVars = new HashMap<>();
                bindVars.put("startVertex", "SoBO/" + randomSoBOId);
                ArangoCursor<HashMap> neighborsResult = database.query(neighborsQuery, bindVars, null, HashMap.class);

                while (neighborsResult.hasNext()) {
                    HashMap<String, Object> record = neighborsResult.next();
                    if (record != null && record.containsKey("neighbor")) {
                        HashMap<String, Object> neighborMap = (HashMap<String, Object>) record.get("neighbor");
                        if (neighborMap != null) {
                            String relationshipType = edgeType;  // Using edgeType as relationship type
                            String croppedNeighborId = neighborMap.get("_id").toString().replace("SoBO/", "");
                            neighbors.append("Neighbor ID: ").append(croppedNeighborId).append(", Relationship: ").append(relationshipType).append("\n");
                        }
                    }
                }
            }
        } else {
            String nodeQuery = "FOR s IN SoBO FILTER s.id == @id RETURN s";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("id", randomSoBOId);
            ArangoCursor<BaseDocument> nodeResult = database.query(nodeQuery, bindVars, null, BaseDocument.class);
            if (nodeResult.hasNext()) {
                BaseDocument sobo = nodeResult.next();
                for (String edgeType : EDGE_TYPES) {
                    String neighborsQuery = "FOR neighbor, edge IN ANY @id " + edgeType + " RETURN {neighbor: neighbor}";

                    bindVars = new MapBuilder().put("id", sobo.getId()).get();
                    ArangoCursor<HashMap> neighborsResult = database.query(neighborsQuery, bindVars, null, HashMap.class);

                    while (neighborsResult.hasNext()) {
                        HashMap<String, Object> record = neighborsResult.next();
                        if (record != null && record.containsKey("neighbor")) {
                            HashMap<String, Object> neighborMap = (HashMap<String, Object>) record.get("neighbor");
                            if (neighborMap != null) {
                                String relationshipType = edgeType;  // Using edgeType as relationship type
                                String croppedNeighborId = neighborMap.get("_id").toString().replace("SoBO/", "");
                                neighbors.append("Neighbor ID: ").append(croppedNeighborId).append(", Relationship: ").append(relationshipType).append("\n");
                            }
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
            return null;
        }
        int randomIndex = new Random().nextInt(soboIds.size());
        return soboIds.get(randomIndex);
    }
    @Override
    public void update() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();
        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform update operation.");
            return;
        }
        soboIds.removeAll(updatedIds);

        if (soboIds.isEmpty()) {
            return;
        }
        String id = getRandomSoBOId(soboIds);
        try {
            if (database.collection("SoBO").documentExists(id)) {
                BaseDocument document = database.collection("SoBO").getDocument(id, BaseDocument.class);
                document.addAttribute("name", "Updated Field");
                database.collection("SoBO").updateDocument(id, document);
                updatedIds.add(id);
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

        String soboIdToDelete = getRandomSoBOId(soboIds);

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

