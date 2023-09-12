package com.example.masterthesisproject;

public interface DatabaseService {
    long create(int minEdgesPerNode, int maxEdgesPerNode);
    void read();
    void update(); // Add percentage parameter here
    void delete();
    void clearDatabase();
    String getDatabaseName();

}