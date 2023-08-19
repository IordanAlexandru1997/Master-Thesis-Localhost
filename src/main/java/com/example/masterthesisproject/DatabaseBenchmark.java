package com.example.masterthesisproject;


import java.util.Random;
public class DatabaseBenchmark {
    private DatabaseService service;
    private int numEntries;

    public DatabaseBenchmark(DatabaseService service, int numEntries) {
        this.service = service;
        this.numEntries = numEntries;
    }

    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete,
                             int minEdgesPerNode, int maxEdgesPerNode) { // Added minEdgesPerNode and maxEdgesPerNode
        if (percentCreate != 0) {
            System.out.println("Clearing SoBO File...");
            SoBOIdTracker.clearSoBOFile();

            System.out.println("Clearing the database...");
            service.clearDatabase();
            System.out.println("Database cleared.");

            // Log the number of records in the database after clearing
            System.out.println("Number of records after clearing: " + service.countRecords());

            System.out.println("Starting the creation process...");
        }
        long startTime = System.nanoTime();

        for (int i = 0; i < (numEntries * percentCreate / 100); i++) {
            System.out.println("Creating SoBOs");
            service.create(minEdgesPerNode, maxEdgesPerNode); // Pass the parameters here
        }

        for (int i = 0; i < (numEntries * percentRead / 100); i++) {
            System.out.println("Reading SoBOs");
            service.read();
        }

        for (int i = 0; i < (numEntries * percentUpdate / 100); i++) {
            System.out.println("Updating SoBOs");
            service.update();
        }

        for (int i = 0; i < (numEntries * percentDelete / 100); i++) {
            System.out.println("Deleting SoBOs");
            service.delete();
        }

        long endTime = System.nanoTime();

        double duration = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Total time taken: " + duration + " seconds");
    }
}