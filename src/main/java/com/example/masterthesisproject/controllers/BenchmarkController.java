package com.example.masterthesisproject.controllers;

import com.example.masterthesisproject.DatabaseBenchmark;
import com.example.masterthesisproject.services.ArangoDBService;
import com.example.masterthesisproject.services.Neo4jService;
import com.example.masterthesisproject.services.OrientDBService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class BenchmarkController {

    @Autowired (required = false)
    private Neo4jService neo4jService;

    @Autowired (required = false)

    private OrientDBService orientDBService;

    @Autowired (required = false)

    private ArangoDBService arangoDBService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/logsPage")
    public String logsPage() {
        return "logs"; // This will render the "logs.html" view
    }


    @GetMapping("/logs")
    public ResponseEntity<String> logs() {
        // Use FileSystemResource to read the template_timings.json content from the root directory
        Resource resource = new FileSystemResource("template_timings.json");
        String content = ""; // Initialize as empty
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String rawContent = new String(bytes, StandardCharsets.UTF_8);

            // Convert the raw content to a list of maps
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> logsList = Arrays.stream(rawContent.split("\\r?\\n"))
                    .filter(line -> !line.isEmpty())
                    .map(line -> {
                        try {
                            return objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.<Map<String, Object>>toList());


            // Convert the list back to a formatted JSON string
            content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logsList);

            // Optional: Write the formatted content back to the file
            // Files.write(Paths.get("template_timings.json"), content.getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return the logs content as a JSON response
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
    }


    @GetMapping("/downloadLogs")
    public ResponseEntity<Resource> downloadLogs() {
        Resource resource = new FileSystemResource("template_timings.json");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"template_timings.json\"")
                .body(resource);
    }
    @PostMapping("/runBenchmark")
    public String runBenchmark(@RequestParam String database,
                               @RequestParam(required = false) boolean optimizeFlag,
                               @RequestParam(defaultValue = "0") int percentCreate,
                               @RequestParam(defaultValue = "0") int percentRead,
                               @RequestParam(defaultValue = "0") int percentUpdate,
                               @RequestParam(defaultValue = "0") int percentDelete,
                               @RequestParam(defaultValue = "0") int numEntries,
                               @RequestParam(defaultValue = "0") int minEdgesPerNode,
                               @RequestParam(defaultValue = "0") int maxEdgesPerNode,
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
