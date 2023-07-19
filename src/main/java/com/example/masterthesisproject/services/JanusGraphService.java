package com.example.masterthesisproject.services;

import com.example.masterthesisproject.entities.Person;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JanusGraphService {
    private final Client client;

    public JanusGraphService() {
        Cluster cluster = Cluster.build()
                .addContactPoint("localhost")
                .port(8182)
                .serializer(Serializers.GRAPHBINARY_V1D0)
                .credentials("janusgraph", "parola")
                .create();
        this.client = cluster.connect();
    }

    public void insertPerson(Person person) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", person.getName());
        params.put("age", person.getAge());
        System.out.println("Inserting person with name: " + person.getName() + " and age: " + person.getAge());
        client.submit("g.addV('person').property('name', name).property('age', age)", params).all().join();
    }

    public Person getPersonsByName(String name) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        List<Result> results = client.submit("g.V().hasLabel('person').has('name', name).valueMap()", params).all().join();
        if (!results.isEmpty()) {
            Result result = results.get(0);
            Map<String, Object> resultMap = result.get(Map.class);
            List<Object> nameList = (List<Object>) resultMap.get("name");
            List<Object> ageList = (List<Object>) resultMap.get("age");
            String personName = (String) nameList.get(0);
            Object ageObject = ageList.get(0);
            Integer personAge;
            if (ageObject instanceof String) {
                personAge = Integer.parseInt((String) ageObject);
            } else if (ageObject instanceof Integer) {
                personAge = (Integer) ageObject;
            } else {
                throw new RuntimeException("Invalid age property type for person with name " + name);
            }
            Person person = new Person();
            person.setName(personName);
            person.setAge(personAge);
            System.out.println("Retrieved person with name: " + person.getName() + " and age: " + person.getAge());
            return person;
        } else {
            System.out.println("Person with name " + name + " not found");
            throw new RuntimeException("Person with name " + name + " not found");
        }
    }

}
