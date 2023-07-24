package com.example.masterthesisproject.entities;

import com.example.masterthesisproject.entities.CreateRelationshipRequest;
import com.example.masterthesisproject.entities.SoBO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public abstract class DatabaseUniversalController {

    // Add a SoBO node to the database
    public long addSoBO(SoBO soboObj, String label) {
        long startTime = System.nanoTime();
        addSoBOImplementation(soboObj, label);
        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    // Implementation is provided by the specific database service
    protected abstract void addSoBOImplementation(SoBO soboObj, String label);

    // Create a relationship between two SoBO nodes in the database
    public long createEdge(CreateRelationshipRequest request) {
        long startTime = System.nanoTime();
        createEdgeImplementation(request);
        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    // Implementation is provided by the specific database service
    protected abstract void createEdgeImplementation(CreateRelationshipRequest request);
}
