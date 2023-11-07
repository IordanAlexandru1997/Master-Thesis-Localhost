package com.example.masterthesisproject;

import com.example.masterthesisproject.services.ArangoDBService;
import com.example.masterthesisproject.services.Neo4jService;
import com.example.masterthesisproject.services.OrientDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;


@SpringBootApplication
public class MasterThesisProjectApplication {

    @Autowired (required = false)
    private Neo4jService neo4jService;

    @Autowired(required = false)
    private OrientDBService orientDBService;

    @Autowired (required = false)
    private ArangoDBService arangoDBService;
    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(MasterThesisProjectApplication.class, args);
    }
    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            Model model = new ExtendedModelMap();

            String[] databases = {"Neo4j", "OrientDB", "ArangoDB"};

            for (String database : databases) {
                for (int numEntries = 1000; numEntries <= 10000; numEntries += 1000) {
                    runBenchmark(database, true, 100, 100, 100, 100, numEntries, 10, 25, model);
                    System.out.println("Benchmark completed for " + database + " with CRUD percentages: 100, 100, 100, 100, and " + numEntries + " entries.");
                }
            }
        };
    }


    public String runBenchmark(String database, boolean optimizeFlag, int percentCreate,
                               int percentRead, int percentUpdate, int percentDelete,
                               int numEntries, int minEdgesPerNode, int maxEdgesPerNode,
                               Model model) {

        if (neo4jService != null) {
            neo4jService.setUiOptimizationFlag(optimizeFlag);
        }
        if (orientDBService != null) {
            orientDBService.setUiOptimizationFlag(optimizeFlag);
        }
        if (arangoDBService != null) {
            arangoDBService.setUiOptimizationFlag(optimizeFlag);
        }
        switch (database) {
            case "Neo4j":
                new DatabaseBenchmark(neo4jService, numEntries, optimizeFlag)
                        .runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, minEdgesPerNode, maxEdgesPerNode);
                break;
            case "OrientDB":
                new DatabaseBenchmark(orientDBService, numEntries, optimizeFlag)
                        .runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, minEdgesPerNode, maxEdgesPerNode);
                break;
            case "ArangoDB":
                new DatabaseBenchmark(arangoDBService, numEntries, optimizeFlag)
                        .runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, minEdgesPerNode, maxEdgesPerNode);
                break;
        }

        model.addAttribute("message", "Benchmark completed for " + database);
        return "index";
    }
}
