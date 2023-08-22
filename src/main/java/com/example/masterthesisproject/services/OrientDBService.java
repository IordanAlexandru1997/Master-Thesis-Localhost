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
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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

    Logger logger = LoggerFactory.getLogger(OrientDBService.class);
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
                    logger.info("Optimized index SoBO_ID_IDX created on SoBO class.");
                } else {
                    logger.info("Running in non-optimized mode. No index created on SoBO class.");
                }

                logger.info("SoBO vertex class created");
            }

            if (db.getClass("WORKS_WITH") == null) {
                db.createClass("WORKS_WITH", "E");
                logger.info("WORKS_WITH edge class created");
            }

            if (db.getClass("FRIENDS_WITH") == null) {
                db.createClass("FRIENDS_WITH", "E");
                logger.info("FRIENDS_WITH edge class created");
            }

            if (db.getClass("RELATED_WITH") == null) {
                db.createClass("RELATED_WITH", "E");
                logger.info("RELATED_WITH edge class created");
            }
            if (db.getClass("RELATED_TO") == null) {
                db.createClass("RELATED_TO", "E");
                logger.info("RELATED_TO edge class created");
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

    public void addSoBOWithSession(SoBO sobo, String idPropertyName, ODatabaseSession db) {
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
        soboVertex.save();
        db.commit();
    }

    @Override
    public void clearDatabase() {
        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            // Delete all edges of type 'RELATED_TO'
            db.command("DELETE EDGE RELATED_TO");

            // Delete all vertices of type 'SoBO'
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

    @Override
    public void create(int minEdgesPerNode, int maxEdgesPerNode) {
        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            SoBO sobo = SoBOGenerator.generateRandomSoBO();
            addSoBOWithSession(sobo, "id", db);
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
                OVertex sobo1Vertex = getOrCreateVertex(edge.getSoboObj1(), db);
                OVertex sobo2Vertex = getOrCreateVertex(edge.getSoboObj2(), db);

                // Check if edge exists
                boolean edgeExists = false;
                try (OResultSet rs = db.query(
                        "SELECT FROM E WHERE out = ? AND in = ? AND @class = ?",
                        sobo1Vertex.getIdentity(), sobo2Vertex.getIdentity(), edge.getType())) {
                    if (rs.hasNext()) {
                        edgeExists = true;
                    }
                }

                // If edge doesn't exist, create it
                if (!edgeExists) {
                    OEdge createdEdge = sobo1Vertex.addEdge(sobo2Vertex, edge.getType());
                    if (edge.getProperties() != null) {
                        for (Map.Entry<String, Object> entry : edge.getProperties().entrySet()) {
                            createdEdge.setProperty(entry.getKey(), entry.getValue());
                        }
                    }
                    createdEdge.save();
                    edgesCreated++;
                    alreadyConnected.add(targetSoBO);  // Mark this node as connected
                }
            }
            db.commit();
            logOperation("Create", "Created SoBO with ID: " + sobo.getId());
        }
    }

    @Override
    public void read() {
        // Load the custom IDs from sobo_obj.json
        List<String> soboIds = SoBOIdTracker.loadSoBOIds();
        StringBuilder neighbors = new StringBuilder();

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated.");
            return;
        }

        // Pick a random custom ID
        String randomSoBOId = soboIds.get(new Random().nextInt(soboIds.size()));
        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            if (isOptimizationEffective()) {
                // Use OrientDB's API to retrieve a vertex using custom ID and its neighbors
                String query = "SELECT expand(out()) FROM SoBO WHERE id = ?";
                OResultSet resultSet = db.query(query, randomSoBOId);

                while (resultSet.hasNext()) {
                    OResult result = resultSet.next();
                    String neighborId = result.getProperty("id");
                    // Relationship type can be inferred based on the edge class, if needed.
                    neighbors.append("Neighbor ID: ").append(neighborId).append(", Relationship: ").append("RELATED_TO").append("\n");  // You can adjust the relationship name as needed
                }

                resultSet.close();
            } else {
                // Non-optimized method (existing approach)
                // Use the picked custom ID to fetch the node
                String nodeQuery = "SELECT FROM V WHERE id = ?";
                OResultSet nodeResult = db.query(nodeQuery, randomSoBOId);

                if (nodeResult.hasNext()) {
                    OResult sobo = nodeResult.next();
//                        System.out.println("Selected SoBO with custom ID: " + sobo.getProperty("id"));  // This will display the custom ID

                    // Fetch the neighbors of the selected SoBO node considering all possible relationships
                    String neighborsQuery = "SELECT expand(outE('RELATED_TO', 'FRIENDS_WITH', 'WORKS_WITH').inV()) FROM V WHERE id = ?";

                    OResultSet neighborsResult = db.query(neighborsQuery, (Object) sobo.getProperty("id"));

                    neighbors = new StringBuilder("Related Neighbors: \n");
                    while (neighborsResult.hasNext()) {
                        OResult record = neighborsResult.next();
                        String neighborId = record.getProperty("id");
                        String relationshipType = record.getProperty("@class");
                        neighbors.append("Neighbor ID: ").append(neighborId).append(", Relationship: ").append(relationshipType).append("\n");
                    }

//                        System.out.println(neighbors.toString());

                } else {
                    System.err.println("No SoBO found for custom ID: " + randomSoBOId);
                }
            }
        }
        logOperation("Read", "Read SoBO with custom ID: " + randomSoBOId);

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

        try (ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            OVertex vertex = getVertexById(db, id);
            if (vertex != null) {
                vertex.setProperty("name", "Updated Field");
                vertex.save();
                updatedIds.add(id); // Add to updated IDs
//                System.out.println("Updated ID: " + id);
            } else {
                System.err.println("Vertex not found for ID: " + id);
            }

        }        logOperation("Update", "Updated SoBO with ID: " + id);

    }


    @Override
    public void delete() {
        List<String> soboIds = SoBOIdTracker.loadSoBOIds(); // Load SoBO IDs

        if (soboIds.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot perform delete operation.");
            return;
        }
//        System.out.println("The list of soboIds is: " + soboIds);
        String id = getRandomSoBOId(soboIds); // Select a random ID from the loaded IDs
//        System.out.println("Selected ID for delete: " + id);
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            db.command("DELETE VERTEX SoBO WHERE id = ?", id);  // Corrected line
        }
//        System.out.println("SoBO deleted: " + id);
        soboIds.remove(id); // Remove the deleted ID from the list
        SoBOIdTracker.saveSoBOIds(soboIds); // Save the updated list back to the file
        logOperation("Delete", "Deleted SoBO with ID: " + id);

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
