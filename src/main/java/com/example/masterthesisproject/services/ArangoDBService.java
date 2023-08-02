package com.example.masterthesisproject.services;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.entity.CollectionType;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.example.masterthesisproject.DatabaseService;
import com.example.masterthesisproject.SoBOGenerator;
import com.example.masterthesisproject.entities.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class ArangoDBService implements DatabaseService {

    private static final String ARANGO_DB_HOST = "localhost";
    private static final int ARANGO_DB_PORT = 8529;
    private static final String ARANGO_DB_USER = "root";
    private static final String ARANGO_DB_PASSWORD = "password";
    private static final String DB_NAME = "arangodb";

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

    @Override
    public void update() {
        SoBO sobo = SoBOGenerator.getRandomSoBO();
        addSoBO(sobo, "id");
    }

    @Override
    public void delete() {
        SoBO sobo = SoBOGenerator.getRandomSoBO();
        if (database.collection("SoBO").documentExists(sobo.getId())) {
            try {
                database.collection("SoBO").deleteDocument(sobo.getId());
                SoBOGenerator.removeSoBO(sobo);  // If deletion is successful, remove SoBO from the list
            } catch (ArangoDBException e) {
                throw new RuntimeException("Failed to delete Document with key: " + sobo.getId() + " from the database.", e);
            }
        } else {
            // Remove the SoBO from GENERATED_SoBOs since it's no longer in the database
            SoBOGenerator.removeSoBO(sobo);
        }
    }



}

