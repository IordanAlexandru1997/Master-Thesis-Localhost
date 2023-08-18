package com.example.masterthesisproject;


import java.util.Random;

public class DatabaseBenchmark {
    private DatabaseService service;
    private int numEntries;

    public DatabaseBenchmark(DatabaseService service, int numEntries) {
        this.service = service;
        this.numEntries = numEntries;
    }

    public void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete) {
        if (percentCreate != 0) {
            SoBOIdTracker.clearSoBOFile();
            service.clearDatabase();
        }

        long startTime = System.nanoTime();

        for (int i = 0; i < (numEntries * percentCreate / 100); i++) {
            System.out.println("Creating SoBOs");
            service.create();
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