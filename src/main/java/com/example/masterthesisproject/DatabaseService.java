package com.example.masterthesisproject;

public interface DatabaseService {
    void create(int minEdgesPerNode, int maxEdgesPerNode);
    void read();
    void update(); // Add percentage parameter here
    void delete();
    void clearDatabase();
    void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete, int numEntries, int minEdges, int maxEdges);


}