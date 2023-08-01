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
        Random rand = new Random();

        // Start timer
        long startTime = System.nanoTime();

        for (int i = 0; i < numEntries; i++) {
            int operation = rand.nextInt(100);

            if (operation < percentCreate) {
                service.create();
            } else if (operation < percentCreate + percentRead) {
                service.read();
            } else if (operation < percentCreate + percentRead + percentUpdate) {
                service.update();
            } else if (operation < percentCreate + percentRead + percentUpdate + percentDelete) {
                service.delete();
            }
        }

        // End timer
        long endTime = System.nanoTime();

        // Calculate duration in seconds
        double duration = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Total time taken: " + duration + " seconds");
    }

}