package com.example.masterthesisproject.services;

import com.arcadedb.database.Database;
import com.example.masterthesisproject.DatabaseBenchmark;
import com.example.masterthesisproject.DatabaseService;
import com.example.masterthesisproject.SoBOGenerator;
import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;



@Service
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('orientdb')}")
public class OrientDBService implements DatabaseService {

    private final String ORIENTDB_URL = "remote:localhost";
    private final String DATABASE_NAME = "orientdb";
    private final String USERNAME = "root";
    private final String PASSWORD = "parola";

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
        String id = getRandomSoBOId();
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            String query = "SELECT FROM SoBO WHERE id = ?";
            OResultSet rs = db.query(query, id);
        }
    }

    @Override
    public void update() {
        String id = getRandomSoBOId();
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            OVertex vertex = getVertexById(db, id);
            if (vertex != null) {
                // Update the properties of the vertex here as needed
                // For example:
                vertex.setProperty("name", "New Name");
                vertex.save();
            }
        }
    }

    @Override
    public void delete() {
        String id = getRandomSoBOId();
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {
            String command = "DELETE VERTEX SoBO WHERE id = ?";
            db.command(new OCommandSQL(command)).execute(id);
        }
    }

    private String getRandomSoBOId() {
        return SoBOGenerator.getRandomSoBOId();
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
    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete, int numEntries) {
        DatabaseBenchmark benchmark = new DatabaseBenchmark(this, numEntries);
        benchmark.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete);
    }

}
