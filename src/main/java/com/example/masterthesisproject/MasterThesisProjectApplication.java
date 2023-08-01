package com.example.masterthesisproject;

import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;
import com.example.masterthesisproject.services.ArangoDBService;
import com.example.masterthesisproject.services.Neo4jService;
import com.example.masterthesisproject.services.OrientDBService;
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
//        OrientDBService orientDBService = context.getBean(OrientDBService.class);
//        ArangoDBService arangoDBService = context.getBean(ArangoDBService.class);

        SoBO sobo1 = new SoBO();

        sobo1.addProperty("id", "1226");
        sobo1.addProperty("name", "Popa Maricica");
        sobo1.addProperty("email", "mail@gmail.com");


        SoBO sobo2 = new SoBO();
        sobo2.addProperty("id", "1238");
        sobo2.addProperty("name", "Marius");
        sobo2.addProperty("email", "mail@gmail.com");
        sobo2.addProperty("yahoo", "madal@gmail.com");
        sobo2.addProperty("outlook", "maadadal@gmail.com");

        SoBO sobo3 = new SoBO();
        sobo3.addProperty("id", "1239");
        sobo3.addProperty("name", "Marius");
        sobo3.addProperty("age", "26");
        sobo3.addProperty("outlook", "maadadal@gmail.com");

        Map<String, Object> edgeProperties = new HashMap<>();
        edgeProperties.put("id", "value1");
        edgeProperties.put("id2", 42);
        Edge edge1 = new Edge(sobo1, sobo2, "RELATED_TO", edgeProperties);
        Edge edge2 = new Edge(sobo2, sobo3, "RELATED_TO");

//        neo4jService.createEdge(edge, "id");

//        orientDBService.addSoBO(sobo1, "id");
//        orientDBService.addSoBO(sobo2, "id");
//        orientDBService.addSoBO(sobo3, "id");
//        orientDBService.createEdge(edge1, "id");
//        orientDBService.createEdge(edge2, "id");

        neo4jService.addSoBO(sobo1, "id");
        neo4jService.addSoBO(sobo2, "id");
//        arangoDBService.addSoBO(sobo1, "id");
//        arangoDBService.addSoBO(sobo2, "id");
//        arangoDBService.addSoBO(sobo3, "id");

// Creating Edge objects
//        arangoDBService.createEdge(edge1, "RELATED_TO");
//        arangoDBService.createEdge(edge2, "RELATED_TO");
        neo4jService.createEdge(edge1, "id");
        neo4jService.createEdge(edge2, "id");
    }

}
