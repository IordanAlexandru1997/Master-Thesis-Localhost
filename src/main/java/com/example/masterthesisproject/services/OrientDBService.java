package com.example.masterthesisproject.services;

import com.example.masterthesisproject.DatabaseBenchmark;
import com.example.masterthesisproject.DatabaseService;
import com.example.masterthesisproject.SoBOGenerator;
import com.example.masterthesisproject.SoBOIdTracker;
import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBO_IDs;
import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBOs;


@Service
@Lazy
public class OrientDBService implements DatabaseService {

    @Value("${orientdb.url}")
    private String ORIENTDB_URL;

    @Value("${orientdb.database}")
    private String DATABASE_NAME;

    @Value("${orientdb.username}")
    private String USERNAME;

    @Value("${orientdb.password}")
    private String PASSWORD;

    Logger logger = LoggerFactory.getLogger(OrientDBService.class);
    private OrientDB orientDB;
    private ODatabaseSession dbSession;

    @PostConstruct
    public void init() {
        initializeSession();
    }

    private void initializeSession() {
        if (orientDB == null) {
            orientDB = new OrientDB(ORIENTDB_URL, USERNAME, PASSWORD, OrientDBConfig.defaultConfig());
        }
        if (dbSession == null) {
            dbSession = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD);
            logger.info("ODatabaseSession initialized or reinitialized.");
        }
        setupSchema();
    }

    private void setupSchema() {

        if (!orientDB.exists(DATABASE_NAME)) {
            orientDB.create(DATABASE_NAME, ODatabaseType.PLOCAL);
        }

        dbSession = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD);

        if (dbSession.getClass("SoBO") == null) {
            OClass soboClass = dbSession.createClass("SoBO");
            soboClass.createProperty("id", OType.STRING);
            soboClass.createIndex("SoBO_ID_IDX", OClass.INDEX_TYPE.UNIQUE, "id");
            logger.info("SoBO vertex class created");
        }
        if (dbSession.getClass("WORKS_WITH") == null) {
            dbSession.createClass("WORKS_WITH", "E");
            logger.info("WORKS_WITH edge class created");
        }
        if (dbSession.getClass("FRIENDS_WITH") == null) {
            dbSession.createClass("FRIENDS_WITH", "E");
            logger.info("FRIENDS_WITH edge class created");
        }
        if (dbSession.getClass("RELATED_WITH") == null) {
            dbSession.createClass("RELATED_WITH", "E");
            logger.info("RELATED_WITH edge class created");
        }
    }


    public void addSoBOWithSession(SoBO sobo, String idPropertyName, ODatabaseSession db) {
        String query = "SELECT FROM SoBO WHERE " + idPropertyName + " = ?";
        OResultSet rs = db.query(query, sobo.getId());

        OVertex soboVertex;
        if (rs.hasNext()) {
            soboVertex = rs.next().getVertex().get();
        } else {
            if (db.getClass("SoBO") == null) {
                OClass soboClass = db.createClass("SoBO");
                soboClass.createIndex("SoBO_ID_IDX", OClass.INDEX_TYPE.UNIQUE, idPropertyName);
            }
            soboVertex = db.newVertex("SoBO");
        }
        soboVertex.setProperty(idPropertyName, sobo.getId());
        for (Map.Entry<String, Object> property : sobo.getProperties().entrySet()) {
            soboVertex.setProperty(property.getKey(), property.getValue());
        }
        soboVertex.save();
        db.commit();
    }

    public void createEdgeWithSession(Edge edge, String id, ODatabaseSession db) {
        // Getting the vertices for soboObj1 and soboObj2
        OVertex sobo1Vertex = getOrCreateVertex(edge.getSoboObj1(), db);
        OVertex sobo2Vertex = getOrCreateVertex(edge.getSoboObj2(), db);
        // Checking if the edge already exists
        OEdge existingEdge = null;
        try (OResultSet rs = db.query(
                "SELECT FROM (TRAVERSE bothE() FROM ?) WHERE @class = ? AND in.@rid = ? AND out.@rid = ?",
                sobo1Vertex.getIdentity(), edge.getType(), sobo2Vertex.getIdentity(), sobo1Vertex.getIdentity())) {
            if (rs.hasNext()) {
                existingEdge = rs.next().getEdge().get();
            }
        }
        // If the edge doesn't exist, create a new one
        if (existingEdge == null) {
            existingEdge = sobo1Vertex.addEdge(sobo2Vertex, edge.getType());
        }
        // Update the properties whether it's an existing or new edge
        if (edge.getProperties() != null) {
            for (Map.Entry<String, Object> entry : edge.getProperties().entrySet()) {
                existingEdge.setProperty(entry.getKey(), entry.getValue());
            }
        }
        existingEdge.save();
    }

    @Override
    public void clearDatabase() {
        try {
            initializeSession();  // Ensure the session is initialized and active

            if (dbSession == null) {
                logger.error("Database session is null. Cannot execute clearDatabase.");
                return;
            }

            OResultSet result = dbSession.command("truncate class SoBO unsafe");
            if (result.hasNext()) {
                System.out.println(result.next());
            } else {
                logger.warn("No result returned from truncate command.");
            }
        } catch (Exception e) {
            logger.error("Error while clearing the database.", e);
        }
    }


    private OVertex getOrCreateVertex(SoBO sobo, ODatabaseSession db) {
        OVertex vertex;
        Object id = sobo.getProperties().get("id");

        if (id == null) {
            throw new IllegalArgumentException("SoBO id cannot be null");
        }

        try (OResultSet rs = db.query("SELECT FROM SoBO WHERE id = ?", id)) {
            if (rs.hasNext()) {
                vertex = rs.next().getVertex().get();
            } else {
                vertex = db.newVertex("SoBO");
                for (Map.Entry<String, Object> entry : sobo.getProperties().entrySet()) {
                    vertex.setProperty(entry.getKey(), entry.getValue());
                }
                vertex.save();
            }
        }

        return vertex;
    }

    @Override
    public void create(int minEdgesPerNode, int maxEdgesPerNode) {
        if (dbSession == null) {
            throw new IllegalStateException("Database session is not initialized.");
        }

        SoBO sobo = SoBOGenerator.generateRandomSoBO();
        addSoBOWithSession(sobo, "id", dbSession);
        GENERATED_SoBOs.add(sobo);
        GENERATED_SoBO_IDs.add(sobo.getId());
        SoBOIdTracker.appendSoBOId(sobo.getId());

        int numEdges = new Random().nextInt(maxEdgesPerNode - minEdgesPerNode + 1) + minEdgesPerNode;

        for (int i = 0; i < numEdges; i++) {
            SoBO targetSoBO = GENERATED_SoBOs.get(new Random().nextInt(GENERATED_SoBOs.size()));
            if (!sobo.equals(targetSoBO)) {
                Edge edge = new Edge(sobo, targetSoBO, "RELATED_TO");
                createEdgeWithSession(edge, "edgeCollection", dbSession);
            }
        }

        dbSession.commit();
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
        String nodeQuery = "SELECT FROM V WHERE id = ?";
        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            OResultSet nodeResult = db.query(nodeQuery, randomSoBOId);

            if (nodeResult.hasNext()) {
                OResult sobo = nodeResult.next();
                System.out.println("Selected SoBO with custom ID: " + sobo.getProperty("id"));  // This will display the custom ID

                // Fetch the neighbors of the selected SoBO node considering all possible relationships
                String neighborsQuery = "SELECT expand(outE('RELATED_TO', 'FRIENDS_WITH', 'WORKS_WITH').inV()) FROM V WHERE id = ?";

                OResultSet neighborsResult = db.query(neighborsQuery, (Object) sobo.getProperty("id"));

                StringBuilder neighbors = new StringBuilder("Related Neighbors: \n");
                while (neighborsResult.hasNext()) {
                    OResult record = neighborsResult.next();
                    String neighborId = record.getProperty("id");
                    String relationshipType = record.getProperty("@class");
                    neighbors.append("Neighbor ID: ").append(neighborId).append(", Relationship: ").append(relationshipType).append("\n");
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

        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            OVertex vertex = getVertexById(db, id);
            if (vertex != null) {
                vertex.setProperty("name", "Updated Field");
                vertex.save();
                updatedIds.add(id); // Add to updated IDs
                System.out.println("Updated ID: " + id);
            } else {
                System.err.println("Vertex not found for ID: " + id);
            }

        }
    }


    @Override
    public void delete() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds(); // Load SoBO IDs

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform delete operation.");
            return;
        }
        System.out.println("The list of soboIds is: " + soboIds);
        String id = getRandomSoBOId(soboIds); // Select a random ID from the loaded IDs
        System.out.println("Selected ID for delete: " + id);
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            dbSession.command("DELETE VERTEX SoBO WHERE id = ?", id);
        }
        System.out.println("SoBO deleted: " + id);
        soboIds.remove(id); // Remove the deleted ID from the list
        SoBOIdTracker.saveSoBOIds(soboIds); // Save the updated list back to the file
    }
    private OVertex getVertexById(ODatabaseSession db, String id) {
        try (OResultSet rs = db.query("SELECT FROM SoBO WHERE id = ?", id)) {
            if (rs.hasNext()) {
                return rs.next().getVertex().get();
            }
        }
        return null;
    }


    public static String getRandomSoBOId(List<String> soboIds) {
        int randomIndex = new Random().nextInt(soboIds.size());
        return soboIds.get(randomIndex);
    }

    private OVertex getVertexById(String id) {
        try (OResultSet rs = dbSession.query("SELECT FROM SoBO WHERE id = ?", id)) {
            if (rs.hasNext()) {
                return rs.next().getVertex().get();
            }
        }
        return null;
    }


    @Override
    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete, int numEntries, int minEdgesPerNode, int maxEdgesPerNode) {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(this, numEntries);
        benchmark.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, minEdgesPerNode, maxEdgesPerNode);
    }


    @PreDestroy
    public void cleanup() {
        if (dbSession != null) {
            dbSession.close();
        }
        if (orientDB != null) {
            orientDB.close();
        }
    }
}
