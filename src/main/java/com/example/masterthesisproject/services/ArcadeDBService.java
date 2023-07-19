package com.example.masterthesisproject.services;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.query.sql.executor.Result;
import com.example.masterthesisproject.entities.Person;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnExpression("#{T(com.example.masterthesisproject.services.DockerContainerChecker).isContainerRunning('arcadedb')}")

public class ArcadeDBService {

    private final String ARCADEDB_URL = "/databases/arcadedb";
    private final String USERNAME = "root";
    private final String PASSWORD = "parola1234";

    private Database database;

    public ArcadeDBService() {
        DatabaseFactory databaseFactory = new DatabaseFactory(ARCADEDB_URL);
        if (!databaseFactory.exists()) {
            this.database = databaseFactory.create();
        } else {
            this.database = databaseFactory.open();
        }
        this.database.command("sql", "CREATE VERTEX TYPE Person IF NOT EXISTS");
        this.database.command("sql", "CREATE EDGE TYPE FRIENDS_WITH IF NOT EXISTS");
    }

    public void insertPerson(Person person) {
        this.database.command("sql", "INSERT INTO Person SET name = ?, age = ?", person.getName(), person.getAge());
    }

    public List<Person> getPersons() {
        ResultSet resultSet = this.database.query("sql", "SELECT FROM Person");
        List<Person> persons = new ArrayList<>();
        while (resultSet.hasNext()) {
            Result result = resultSet.next();
            if (result.getElement().isPresent()) {
                MutableDocument vertex = (MutableDocument) result.getElement().get().asDocument();
                Person person = new Person(vertex.get("name").toString(), Integer.parseInt(vertex.get("age").toString()));
                persons.add(person);
            }
        }
        return persons;
    }
}
