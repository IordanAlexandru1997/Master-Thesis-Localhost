package com.example.masterthesisproject.services;

import com.example.masterthesisproject.entities.Person;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

// Connection string:
// docker run --rm -p 2424:2424 --env JAVA_OPTS="-Darcadedb.server.rootPassword=playwithdata" arcadedata/arcadedb:latest


// Database needs to be setup on the interface
// minimum 8 chars for the password
// insightful logs in terminal
// Important note, hashed localhost address to the interface in the terminal is not working


//CREATE VERTEX TYPE Person
//CREATE EDGE TYPE Friends
//INSERT INTO Person CONTENT { "name": "Alice", "age": 30 };
//INSERT INTO Person CONTENT { "name": "Bob", "age": 35 };
//CREATE EDGE Friends FROM (SELECT FROM Person WHERE name = 'Alice') TO (SELECT FROM Person WHERE name = 'Bob')

@Service
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('arcadedb')}")

public class ArcadeDBService {
    private static final String ARCADEDB_URL = "http://localhost:2480";
    private static final String DATABASE_NAME = "PersonDB";
    private static final String COLLECTION_NAME = "Person";

    private final RestTemplate restTemplate = new RestTemplate();

    public void createDatabase() {
        try {
            String query = String.format("CREATE DATABASE %s", DATABASE_NAME);
            executeQuery(query);
        } catch (HttpClientErrorException e) {
            // Handle the exception
        }
    }

    public void createCollection() {
        try {
            String query = String.format("CREATE VERTEX TYPE %s", COLLECTION_NAME);
            executeQuery(query);
        } catch (HttpClientErrorException e) {
            // Handle the exception
        }
    }

    public void insertPerson(Person person) {
        String query = String.format(
                "INSERT INTO %s CONTENT {\"name\": \"%s\", \"age\": %d}",
                COLLECTION_NAME, person.getName(), person.getAge());

        executeQuery(query);
    }

    public String getPersonsByName(String name) {
        String query = String.format(
                "SELECT FROM %s WHERE name = \"%s\"",
                COLLECTION_NAME, name);

        return executeQuery(query);
    }

    private String executeQuery(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("root", "parola1234");

        HttpEntity<String> entity = new HttpEntity<>(query, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                ARCADEDB_URL + "/query/" + DATABASE_NAME + "/sql",
                HttpMethod.POST, entity, String.class);

        return response.getBody();
    }
}