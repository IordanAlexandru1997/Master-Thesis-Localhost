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
import java.util.*;


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


    @PostConstruct
    public void init() {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, USERNAME, PASSWORD, OrientDBConfig.defaultConfig())) {
            if (!orientDB.exists(DATABASE_NAME)) {
                orientDB.create(DATABASE_NAME, ODatabaseType.PLOCAL);
            }
            try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
                if (db.getClass("SoBO") == null) {
                    OClass soboClass = db.createClass("SoBO");
                    soboClass.createProperty("id", OType.STRING);
                    soboClass.createIndex("SoBO_ID_IDX", OClass.INDEX_TYPE.UNIQUE, "id");
                    logger.info("SoBO vertex class created");
                }
                if (db.getClass("WORKS_WITH") == null) {
                    OClass worksWithEdgeClass = db.createClass("WORKS_WITH", "E");
                    logger.info("WORKS_WITH edge class created");
                }
                if (db.getClass("FRIENDS_WITH") == null) {
                    OClass friendsWithEdgeClass = db.createClass("FRIENDS_WITH", "E");
                    logger.info("FRIENDS_WITH edge class created");
                }
                if (db.getClass("RELATED_WITH") == null) {
                    OClass friendsWithEdgeClass = db.createClass("RELATED_WITH", "E");
                    logger.info("RELATED_WITH edge class created");
                }
            }
        } catch (Exception e) {
            logger.error("Error during init", e);
        }
    }

    public void addSoBO(SoBO sobo, String idPropertyName) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
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
    }
    public void createEdge(Edge edge, String id) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            // Getting the vertices for soboObj1 and soboObj2
            OVertex sobo1Vertex = getOrCreateVertex(db, edge.getSoboObj1());
            OVertex sobo2Vertex = getOrCreateVertex(db, edge.getSoboObj2());
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
    }
    @Override
    public void clearDatabase() {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            OResultSet result =db.command("truncate class SoBO unsafe");
            System.out.println(result.next());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to cleanup OrientDB database.", e);
        }
    }


    private OVertex getOrCreateVertex(ODatabaseSession db, SoBO sobo) {
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
    private static int soboCounter = 0;
    public void create(int minEdgesPerNode, int maxEdgesPerNode) {

        SoBO sobo = SoBOGenerator.generateRandomSoBO();
        addSoBO(sobo, "id");

        soboCounter++;
        if (soboCounter >= 2) {
            Edge edge = SoBOGenerator.generateRandomEdge();
            createEdge(edge, "edgeCollection");
            soboCounter = 0;
        }
        System.out.println("SoBO created: " + sobo.getId());
        SoBOIdTracker.appendSoBOId(sobo.getId());// Save the generated SoBO IDs
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
        try (ODatabaseSession db = new ODatabaseDocumentTx(ORIENTDB_URL + "/" + DATABASE_NAME).open(USERNAME, PASSWORD)) {
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

        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
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
            String command = "DELETE VERTEX SoBO WHERE id = ?";
            db.command(new OCommandSQL(command)).execute(id);
        }
        System.out.println("SoBO deleted: " + id);
        soboIds.remove(id); // Remove the deleted ID from the list
        SoBOIdTracker.saveSoBOIds(soboIds); // Save the updated list back to the file
    }


    public static String getRandomSoBOId(List<String> soboIds) {
        int randomIndex = new Random().nextInt(soboIds.size());
        return soboIds.get(randomIndex);
    }


    private OVertex getVertexById(ODatabaseSession db, String id) {
        try (OResultSet rs = db.query("SELECT FROM SoBO WHERE id = ?", id)) {
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


}
