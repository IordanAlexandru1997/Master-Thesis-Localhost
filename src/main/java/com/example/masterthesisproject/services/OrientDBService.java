package com.example.masterthesisproject.services;

import com.example.masterthesisproject.DatabaseService;
import com.example.masterthesisproject.SoBOGenerator;
import com.example.masterthesisproject.SoBOIdTracker;
import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBO_IDs;
import static com.example.masterthesisproject.SoBOGenerator.GENERATED_SoBOs;


@Service
@Lazy
public class OrientDBService implements DatabaseService {
    private static final String OPERATIONAL_LOG_FILE = "operational_logs.json";

    @Value("${orientdb.url}")
    private String ORIENTDB_URL;

    @Value("${orientdb.database}")
    private String DATABASE_NAME;

    @Value("${orientdb.username}")
    private String USERNAME;

    @Value("${orientdb.password}")
    private String PASSWORD;

    @Value("${optimization.enabled}")
    private boolean optimizationEnabled;
    private OrientDB orientDB;
    private Boolean uiOptimizationFlag = null;

    public boolean isOptimizationEffective() {
        return uiOptimizationFlag != null ? uiOptimizationFlag : optimizationEnabled;
    }

    public void setUiOptimizationFlag(boolean flag) {
        this.uiOptimizationFlag = flag;
    }

    @PostConstruct
    public void init() {

        initializeSession();
    }

    private void initializeSession() {
        if (orientDB == null) {
            orientDB = new OrientDB(ORIENTDB_URL, USERNAME, PASSWORD, OrientDBConfig.defaultConfig());
        }
        setupSchema();
    }

    private void setupSchema() {
        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            if (db.getClass("SoBO") == null) {
                OClass soboClass = db.createClass("SoBO", "V");  // Extend the default V class
                soboClass.createProperty("id", OType.STRING);

                if (isOptimizationEffective()) {
                    soboClass.createIndex("SoBO_ID_IDX", OClass.INDEX_TYPE.UNIQUE, "id");
                } else {
                }

            }

            if (db.getClass("WORKS_WITH") == null) {
                db.createClass("WORKS_WITH", "E");
            }

            if (db.getClass("FRIENDS_WITH") == null) {
                db.createClass("FRIENDS_WITH", "E");
            }

            if (db.getClass("RELATED_TO") == null) {
                db.createClass("RELATED_TO", "E");
            }


        }
    }

    private void logOperation(String operation, String message) {
        try (FileWriter file = new FileWriter(OPERATIONAL_LOG_FILE, true)) {
            JsonObjectBuilder logObjectBuilder = Json.createObjectBuilder();
            logObjectBuilder.add("database", "OrientDB")
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
        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            db.command("DELETE EDGE RELATED_TO");

            db.command("DELETE VERTEX SoBO");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to clear OrientDB database.", e);
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
    public long addSoBOWithSession(SoBO sobo, String idPropertyName, ODatabaseSession db) {
        String query = "SELECT FROM SoBO WHERE " + idPropertyName + " = ?";
        OResultSet rs = db.query(query, sobo.getId());

        OVertex soboVertex;
        if (rs.hasNext()) {
            soboVertex = rs.next().getVertex().get();
        } else {
            soboVertex = db.newVertex("SoBO");
        }

        soboVertex.setProperty(idPropertyName, sobo.getId());
        for (Map.Entry<String, Object> property : sobo.getProperties().entrySet()) {
            soboVertex.setProperty(property.getKey(), property.getValue());
        }

        long startInsertionTime = System.currentTimeMillis();
        soboVertex.save();
        db.commit();
        long endInsertionTime = System.currentTimeMillis();

        return  endInsertionTime - startInsertionTime;

    }

    @Override
    public long create(int minEdgesPerNode, int maxEdgesPerNode) {
        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            SoBO sobo = SoBOGenerator.generateRandomSoBO();
            long insertionDuration = addSoBOWithSession(sobo, "id", db);

            GENERATED_SoBOs.add(sobo);
            GENERATED_SoBO_IDs.add(sobo.getId());
            SoBOIdTracker.appendSoBOId(sobo.getId());
            int numEdgesToCreate =(int) Math.round((minEdgesPerNode + maxEdgesPerNode) / 2.0);

            int edgesCreated = 0;

            Set<SoBO> alreadyConnected = new HashSet<>();
            alreadyConnected.add(sobo);

            List<SoBO> potentialConnections = new ArrayList<>(GENERATED_SoBOs);
            Collections.shuffle(potentialConnections);

            for (SoBO targetSoBO : potentialConnections) {
                if (edgesCreated == numEdgesToCreate) break;
                if (alreadyConnected.contains(targetSoBO)) {
                    continue;
                }
                Edge edge = SoBOGenerator.generateRandomEdge(sobo, targetSoBO);
                String edgeType = (String) edge.getProperties().get("edgeType");



                OVertex sobo1Vertex = getOrCreateVertex(edge.getSoboObj1(), db);
                OVertex sobo2Vertex = getOrCreateVertex(edge.getSoboObj2(), db);

                // Check if edge exists
                boolean edgeExists = false;
                try (OResultSet rs = db.query(
                        "SELECT FROM E WHERE out = ? AND in = ? AND @class = ?",
                        sobo1Vertex.getIdentity(), sobo2Vertex.getIdentity(), edgeType)) {
                    if (rs.hasNext()) {
                        edgeExists = true;
                    }
                }

                // If edge doesn't exist, create it
                if (!edgeExists) {
                    OEdge createdEdge = sobo1Vertex.addEdge(sobo2Vertex, edgeType);
                    if (edge.getProperties() != null) {
                        for (Map.Entry<String, Object> entry : edge.getProperties().entrySet()) {
                            createdEdge.setProperty(entry.getKey(), entry.getValue());
                        }
                    }
                    createdEdge.save();
                    edgesCreated++;
                    alreadyConnected.add(targetSoBO);
                }
            }
            db.commit();
            logOperation("Create", "Created SoBO with ID: " + sobo.getId());
            return insertionDuration;
        }
    }


    @Override
    public void read() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();
        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated.");
            return;
        }
        StringBuilder details = new StringBuilder();
        StringBuilder neighbors = new StringBuilder();

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated.");
            return;
        }

        // Pick a random custom ID
        String randomSoBOId = soboIds.get(new Random().nextInt(soboIds.size()));
        details.append("Selected SoBO with ID: ").append(randomSoBOId).append("; Neighbors: ");


        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            if (isOptimizationEffective()) {
                String query = "SELECT expand(both()) FROM SoBO WHERE id = ?";
                OResultSet resultSet = db.query(query, randomSoBOId);
                int neighborCount = 0;

                while (resultSet.hasNext()) {
                    OResult result = resultSet.next();
                    String neighborId = result.getProperty("id");
                    neighbors.append("Neighbor ID: ").append(neighborId).append(", Relationship: ").append("RELATED_TO").append("\n");
                    details.append("\nNeighbor ID: ").append(neighborId).append(", Relationship: RELATED_TO");  // Adjusted the relationship name as "RELATED_TO"
                    neighborCount++;

                }
                details.append("\nTotal Neighbors Retrieved: " + neighborCount);

                resultSet.close();
            } else {
                // Non-optimized method (existing approach)
                // Use the picked custom ID to fetch the node
                String nodeQuery = "SELECT FROM V WHERE id = ?";
                OResultSet nodeResult = db.query(nodeQuery, randomSoBOId);

                if (nodeResult.hasNext()) {
                    OResult sobo = nodeResult.next();
                    String neighborsQuery = "SELECT expand(unionall(outE('RELATED_TO', 'FRIENDS_WITH', 'WORKS_WITH').inV(), inE('RELATED_TO', 'FRIENDS_WITH', 'WORKS_WITH').outV())) FROM V WHERE id = ?";

                    OResultSet neighborsResult = db.query(neighborsQuery, (Object) sobo.getProperty("id"));

                    neighbors = new StringBuilder("Related Neighbors: \n");
                    while (neighborsResult.hasNext()) {
                        OResult record = neighborsResult.next();
                        String neighborId = record.getProperty("id");
                        String relationshipType = record.getProperty("@class");
                        neighbors.append("Neighbor ID: ").append(neighborId).append(", Relationship: ").append(relationshipType).append("\n");
                        details.append("\nNeighbor ID: ").append(neighborId).append(", Relationship: ").append(relationshipType);

                    }
                } else {
                    System.err.println("No SoBO found for custom ID: " + randomSoBOId);
                }
            }

        }
        logOperation("Read", details.toString());

    }





    private final List<String> updatedIds = new ArrayList<>();
    public void update() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds(); // Load SoBO IDs

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform update operation.");
            return;
        }

        soboIds.removeAll(updatedIds);

        if (soboIds.isEmpty()) {
            return;
        }

        String id = getRandomSoBOId(soboIds);

        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            // Using SQL query for UPSERT operation
            String query = "UPDATE SoBO SET name = ? UPSERT WHERE id = ?";
            OResultSet resultSet = db.command(query, "Updated Field", id);

            if (resultSet.hasNext()) {
                OResult result = resultSet.next();
                // This will retrieve the updated or inserted OVertex (record) if you need it
                OVertex vertex = result.getVertex().orElse(null);
            }

            resultSet.close();

            updatedIds.add(id);
            logOperation("Update", "Updated or created SoBO with ID: " + id);
        }
    }




    @Override
    public void delete() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform delete operation.");
            return;
        }

        String soboIdToDelete = getRandomSoBOId(soboIds);

        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            // Check if vertex exists, and then delete it
            OResultSet resultSet = db.query("SELECT FROM SoBO WHERE id = ?", soboIdToDelete);
            if (resultSet.hasNext()) {
                db.command("DELETE VERTEX SoBO WHERE id = ?", soboIdToDelete);
            }
        }

        soboIds.remove(soboIdToDelete);
        SoBOIdTracker.saveSoBOIds(soboIds);
        logOperation("Delete", "Deleted SoBO with ID: " + soboIdToDelete);
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
    public String getDatabaseName() {
        return "OrientDB";
    }


    @PreDestroy
    public void cleanup() {
        if (orientDB != null) {
            orientDB.close();
        }
    }
}
