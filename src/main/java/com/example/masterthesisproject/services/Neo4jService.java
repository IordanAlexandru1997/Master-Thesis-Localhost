package com.example.masterthesisproject.services;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.masterthesisproject.repositories.PersonRepository;
import com.example.masterthesisproject.entities.Person;

import java.util.List;
import java.util.Map;

@Service
public class Neo4jService {

    private final Neo4jClient neo4jClient;

    public Neo4jService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void createPersonWithRelationship(String personName, String friendName) {
        String query = "MERGE (p:Person {name: $personName}) " +
                "MERGE (f:Person {name: $friendName}) " +
                "MERGE (p)-[:FRIENDS_WITH]->(f)";
        neo4jClient.query(query)
                .bind(personName).to("personName")
                .bind(friendName).to("friendName")
                .run();
    }

    public List<Person> getFriendsOfPerson(String personName) {
        String query = "MATCH (p:Person {name: $personName})-[:FRIENDS_WITH]->(f) RETURN f";
        return (List<Person>) neo4jClient.query(query)
                .bind(personName).to("personName")
                .fetchAs(Person.class)
                .mappedBy((typeSystem, record) -> {
                    var node = record.get("f").asNode();
                    var person = new Person();
                    person.setName(node.get("name").asString());
                    person.setAge(node.get("age").asInt());
                    return person;
                })
                .all();
    }

}