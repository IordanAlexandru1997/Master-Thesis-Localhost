package com.example.masterthesisproject.services;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrientDBService {

    private final String ORIENTDB_URL = "remote:localhost";
    private final String DATABASE_NAME = "orientdb";
    private final String USERNAME = "root";
    private final String PASSWORD = "password";

    @PostConstruct
    public void init() {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig())) {
            if (!orientDB.exists(DATABASE_NAME)) {
                orientDB.create(DATABASE_NAME, ODatabaseType.PLOCAL);
            }
        }
    }

    public void insertPerson(String name, int age) {
        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            OElement person = db.newElement("Person");
            person.setProperty("name", name);
            person.setProperty("age", age);
            person.save();
        }
    }

    public List<OElement> getPersonsByName(String name) {
        List<OElement> persons = new ArrayList<>();

        try (OrientDB orientDB = new OrientDB(ORIENTDB_URL, OrientDBConfig.defaultConfig());
             ODatabaseSession db = orientDB.open(DATABASE_NAME, USERNAME, PASSWORD)) {

            String query = "SELECT * FROM Person WHERE name = ?";
            try (OResultSet resultSet = db.query(query, name)) {
                while (resultSet.hasNext()) {
                    persons.add(resultSet.next().toElement());
                }
            }
        }

        return persons;
    }
}
