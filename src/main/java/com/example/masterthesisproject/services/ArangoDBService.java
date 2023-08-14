package com.example.masterthesisproject.services;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.entity.CollectionType;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
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


    private ArangoDB arangoDB;
    private ArangoDatabase database;

    @PostConstruct
    public void init() {
        arangoDB = new ArangoDB.Builder()
                .host(ARANGO_DB_HOST, ARANGO_DB_PORT)
                .user(ARANGO_DB_USER)
                .password(ARANGO_DB_PASSWORD)
                .build();
        database = arangoDB.db(DB_NAME);
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

    private static int soboCounter = 0;

    @Override
    public void create() {
        SoBO sobo = SoBOGenerator.generateRandomSoBO();
        addSoBO(sobo, "id");

        soboCounter++;
        if (soboCounter >= 2) {
            Edge edge = SoBOGenerator.generateRandomEdge();
            createEdge(edge, "edgeCollection");
            soboCounter = 0;
        }
    }


    @Override
    public void read() {
        SoBO sobo = SoBOGenerator.getRandomSoBO();
        String key = (String) sobo.getProperties().get("id");
        database.collection("SoBO").getDocument(key, BaseDocument.class);
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
    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete, int numEntries) {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(this, numEntries);
        benchmark.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete);
    }

}

