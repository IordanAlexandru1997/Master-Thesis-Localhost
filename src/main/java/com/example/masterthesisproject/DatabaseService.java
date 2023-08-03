package com.example.masterthesisproject;

public interface DatabaseService {
    void create();
    void read();
    void update();
    void delete();
    void runBenchmark(int percentCreate, int percentRead, int percentUpdate, int percentDelete, int numEntries);

}