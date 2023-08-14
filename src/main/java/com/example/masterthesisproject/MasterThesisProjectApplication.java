package com.example.masterthesisproject;

import com.example.masterthesisproject.services.ArangoDBService;
import com.example.masterthesisproject.services.Neo4jService;
import com.example.masterthesisproject.services.OrientDBService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class MasterThesisProjectApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(MasterThesisProjectApplication.class, args);
        Neo4jService neo4jService = context.getBean(Neo4jService.class);
//        OrientDBService orientDBService = context.getBean(OrientDBService.class);
//        ArangoDBService arangoDBService = context.getBean(ArangoDBService.class);

//        orientDBService.runBenchmark(100, 0, 0 , 0, 100);
        neo4jService.runBenchmark(0, 0, 0, 100, 400);
//        arangoDBService.runBenchmark(100, 0, 0, 0 , 80);
    }
}
