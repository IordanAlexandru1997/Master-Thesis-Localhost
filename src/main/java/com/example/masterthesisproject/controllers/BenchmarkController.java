package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.services.ArangoDBService;
import com.example.masterthesisproject.services.Neo4jService;
import com.example.masterthesisproject.services.OrientDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BenchmarkController {

    @Autowired
    private Neo4jService neo4jService;

    @Autowired
    private OrientDBService orientDBService;

    @Autowired
    private ArangoDBService arangoDBService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/runBenchmark")
    public String runBenchmark(@RequestParam String database,
                               @RequestParam int percentCreate,
                               @RequestParam int percentRead,
                               @RequestParam int percentUpdate,
                               @RequestParam int percentDelete,
                               @RequestParam int numEntries,
                               Model model) {

        switch (database) {
            case "Neo4j":
                neo4jService.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, numEntries);
                break;
            case "OrientDB":
                orientDBService.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, numEntries);
                break;
            case "ArangoDB":
                arangoDBService.runBenchmark(percentCreate, percentRead, percentUpdate, percentDelete, numEntries);
                break;
        }

        model.addAttribute("message", "Benchmark completed for " + database);
        return "index";
    }
}
