package com.example.masterthesisproject;

import com.example.masterthesisproject.entities.CreateRelationshipRequest;
import com.example.masterthesisproject.entities.SoBO;
import com.example.masterthesisproject.services.Neo4jService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

@SpringBootApplication
public class MasterThesisProjectApplication {

    public static void main(String[] args) {
        // Launch the Spring Boot application
        ConfigurableApplicationContext context = SpringApplication.run(MasterThesisProjectApplication.class, args);

        // Get the Neo4jService bean
        Neo4jService neo4jService = context.getBean(Neo4jService.class);

        // Now, you can use the neo4jService object to use the methods in the Neo4jService class.

        // As an example, we can use the `createEmployee` method in the `Neo4jService` class.
        neo4jService.createEmployee("John Doe", 50000, "1", "Engineering");

        // Or you can create a new SoBO and add it to the database
        SoBO sobo3 = new SoBO();
        sobo3.addProperty("name", "Nadim");
        sobo3.addProperty("email", "mail@gmail.com");
        neo4jService.addSoBO(sobo3, "Emp");
        SoBO sobo4 = new SoBO();
        sobo4.addProperty("name", "Alex");
        sobo4.addProperty("email", "mail@gmail.com");
        neo4jService.addSoBO(sobo4, "Emp");
        Map<String, Object> sobo3Properties = sobo3.getProperties();
        Map<String, Object> sobo4Properties = sobo4.getProperties();
        CreateRelationshipRequest request = new CreateRelationshipRequest(sobo3Properties, sobo4Properties, "email", "Emp", "Emp");
        neo4jService.createEdge(request);

    }

}
