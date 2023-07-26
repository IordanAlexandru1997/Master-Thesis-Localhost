package com.example.masterthesisproject;

import com.example.masterthesisproject.entities.CreateRelationshipRequest;
import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;
import com.example.masterthesisproject.services.Neo4jService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class MasterThesisProjectApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MasterThesisProjectApplication.class, args);
        Neo4jService neo4jService = context.getBean(Neo4jService.class);

        SoBO sobo1 = new SoBO();
        sobo1.addProperty("id", "1234");
        sobo1.addProperty("name", "Papusoi Mariusoi");
        sobo1.addProperty("email", "mail@gmail.com");
        neo4jService.addSoBO(sobo1, "id");

        SoBO sobo2 = new SoBO();
        sobo2.addProperty("id", "1235");
        sobo2.addProperty("name", "Covrig Andreioi");
        sobo2.addProperty("email", "mail@gmail.com");
        neo4jService.addSoBO(sobo2, "id");

        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("property1", "value1");
        edgeProperties.put("property2", 42);
        Edge edge = new Edge(sobo1, sobo2, "RELATED_TO", edgeProperties);

        neo4jService.createEdge(edge, "id");
    }

}
