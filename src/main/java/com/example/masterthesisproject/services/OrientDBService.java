package com.example.masterthesisproject.services;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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
                if (db.getClass("Employee") == null) {
                    db.createVertexClass("Employee");
                    logger.info("Employee vertex class created");
                }
                if (db.getClass("Invoice") == null) {
                    db.createVertexClass("Invoice");
                    logger.info("Invoice vertex class created");
                }
                if (db.getClass("Project") == null) {
                    db.createVertexClass("Project");
                    logger.info("Project vertex class created");
                }
                if (db.getClass("worksOn") == null) {
                    db.createEdgeClass("worksOn");
                    logger.info("worksOn edge class created");
                }
                if (db.getClass("issues") == null) {
                    db.createEdgeClass("issues");
                    logger.info("issues edge class created");
                }
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
}
