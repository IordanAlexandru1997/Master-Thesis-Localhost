package com.example.masterthesisproject;

public interface DatabaseService {
    long create(int minEdgesPerNode, int maxEdgesPerNode);
    void read();
    void update();
    void delete();
    void clearDatabase();
    String getDatabaseName();

}