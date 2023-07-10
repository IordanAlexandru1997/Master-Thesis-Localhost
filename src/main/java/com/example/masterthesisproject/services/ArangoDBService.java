package com.example.masterthesisproject.services;

import com.arangodb.ArangoDB;
import com.example.masterthesisproject.entities.Person;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.arangodb.util.MapBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;
//Very easy installation
@Service
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('arangodb')}")
public class ArangoDBService {

    private static final String DATABASE_NAME = "PersonDB";
    private static final String COLLECTION_NAME = "Person";

    @Bean
    public ArangoDB arangoDB() {
        ArangoDB arangoDB = new ArangoDB.Builder()
                .host("localhost", 8529)
                .user("root")
                .password("parola")
                .build();

        // Create the database if it doesn't exist
        if (!arangoDB.getDatabases().contains(DATABASE_NAME)) {
            arangoDB.createDatabase(DATABASE_NAME);
        }

        return arangoDB;
    }

    @Bean
    public ArangoDatabase arangoDatabase(ArangoDB arangoDB) {
        ArangoDatabase database = arangoDB.db(DATABASE_NAME);

        // Create the collection if it doesn't exist
        if (database.getCollections().stream().noneMatch(collection -> collection.getName().equals(COLLECTION_NAME))) {
            database.createCollection(COLLECTION_NAME);
        }

        return database;
    }

    @Autowired
    private ArangoDatabase arangoDatabase;

    public void insertPerson(String name, int age) {
        BaseDocument myObject = new BaseDocument();
        myObject.addAttribute("name", name);
        myObject.addAttribute("age", age);
        arangoDatabase.collection(COLLECTION_NAME).insertDocument(myObject);
    }

    public List<Person> getPersonsByName(String name) {
        return arangoDatabase.query("FOR p IN " + COLLECTION_NAME + " FILTER p.name == @name RETURN p", new MapBuilder().put("name", name).get(), null, Person.class).asListRemaining();
    }
}
