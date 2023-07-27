package com.example.masterthesisproject.services;

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
public class OrientDBService {

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
                // ... remaining code ...
            }
        } catch (Exception e) {
            logger.error("Error during init", e);
        }
    }


    public void insertEmployee(String name, double salary, String id, String department) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            OVertex employee = db.newVertex("Employee");
            employee.setProperty("name", name);
            employee.setProperty("salary", salary);
            employee.setProperty("id", id);
            employee.setProperty("department", department);
            employee.save();
        }
    }

    public void insertProject(String id, String name) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            OVertex project = db.newVertex("Project");
            project.setProperty("id", id);
            project.setProperty("name", name);
            project.save();
        }
    }

    public void insertInvoice(String id, String customer, double amount) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            OVertex invoice = db.newVertex("Invoice");
            invoice.setProperty("id", id);
            invoice.setProperty("customer", customer);
            invoice.setProperty("amount", amount);
            invoice.save();
        }
    }
    public void insertEmployeeToProject(String employeeId, String projectId) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            String command = "CREATE EDGE worksOn FROM (SELECT FROM Employee WHERE id = ?) TO (SELECT FROM Project WHERE id = ?)";
            db.command(new OCommandSQL(command)).execute(employeeId, projectId);
        }
    }

    public void insertEmployeeToInvoice(String employeeId, String invoiceId) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            String command = "CREATE EDGE issues FROM (SELECT FROM Employee WHERE id = '" + employeeId + "') TO (SELECT FROM Invoice WHERE id = '" + invoiceId + "')";
            db.command(command);

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


}
