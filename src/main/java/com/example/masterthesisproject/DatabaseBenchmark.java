package com.example.masterthesisproject;

import org.springframework.beans.factory.annotation.Value;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DatabaseBenchmark {
    private final DatabaseService service;
    private final int numEntries;

    @Value("${optimization.enabled}")
    private boolean optimizationEnabled;

    private Boolean uiOptimizationFlag = null;

    public boolean isOptimizationEffective() {
        return uiOptimizationFlag != null ? uiOptimizationFlag : optimizationEnabled;
    }

    public DatabaseBenchmark(DatabaseService service, int numEntries, boolean optimizeFlag) {
        this.service = service;
        this.numEntries = numEntries;
        this.uiOptimizationFlag = optimizeFlag;
    }

    private void logOperation(String operation, int percent, double duration, int records, int minEdges, int maxEdges, FileWriter file) throws IOException {
        double roundedDuration = roundToThreeDecimals(duration);

        if(roundedDuration == 0) {
            return;
        }

        JsonObjectBuilder logObjectBuilder = Json.createObjectBuilder();
        logObjectBuilder.add("operationDetails", Json.createObjectBuilder()
                .add("database_name", service.getDatabaseName())
                .add("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                .add("operation", operation)
                .add("percent", percent)
                .add("duration", roundedDuration)
                .add("records", records)
                .add("min_edges_per_node", minEdges)
                .add("max_edges_per_node", maxEdges)
                .add("optimization", isOptimizationEffective() ? "Yes" : "No"));
        file.write(logObjectBuilder.build().toString() + "\n");
    }


    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete,
                             int minEdgesPerNode, int maxEdgesPerNode) {
        SoBOGenerator.initializeCreation();

        if (percentCreate != 0) {
            SoBOIdTracker.clearSoBOFile();
            service.clearDatabase();
        }

        System.out.println("Starting operation timing for " + service.getDatabaseName());
        try (FileWriter file = new FileWriter("template_timings.json", true)) {
            double totalInsertionTime = 0.0;

            for (int i = 0; i < (numEntries * percentCreate / 100); i++) {
                totalInsertionTime += service.create(minEdgesPerNode, maxEdgesPerNode); // capture the insertion time
            }
            totalInsertionTime = totalInsertionTime / 1000; // Convert to seconds
            logOperation("Insertion Time", percentCreate, totalInsertionTime, numEntries * percentCreate / 100, minEdgesPerNode, maxEdgesPerNode, file);


            Map<String, Runnable> operations = Map.of(
                    "Read", service::read,
                    "Update", service::update,
                    "Delete", service::delete
            );

            Map<String, Integer> percentages = Map.of(
                    "Read", percentRead,
                    "Update", percentUpdate,
                    "Delete", percentDelete
            );

            for (String operation : List.of("Read", "Update", "Delete")) {
                int recordsAffected = numEntries * percentages.get(operation) / 100;

                long startTime = System.nanoTime();
                for (int i = 0; i < (numEntries * percentages.get(operation) / 100); i++) {
                    operations.get(operation).run(); // crucial line that runs the "runner"
                }
                long endTime = System.nanoTime();
                double duration = (endTime - startTime) / 1_000_000_000.0;
                logOperation(operation, percentages.get(operation), duration, recordsAffected, minEdgesPerNode, maxEdgesPerNode, file);

            }

            System.out.println("Process finished.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private double roundToThreeDecimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

}