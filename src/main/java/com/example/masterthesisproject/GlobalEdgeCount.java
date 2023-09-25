package com.example.masterthesisproject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalEdgeCount {
    private static GlobalEdgeCount instance;
    private static final Logger logger = LoggerFactory.getLogger(GlobalEdgeCount.class);

    private int numEdgesToCreate;

    private GlobalEdgeCount() {
    }

    public static synchronized GlobalEdgeCount getInstance() {
        if (instance == null) {
            instance = new GlobalEdgeCount();
        }
        return instance;
    }

    public int getNumEdgesToCreate() {
        return numEdgesToCreate;
    }

    public void setNumEdgesToCreate(int minEdges, int maxEdges) {

        this.numEdgesToCreate = (int) Math.floor((minEdges + maxEdges) / 2.0);
        logger.info("Global edge count set to {}", numEdgesToCreate);

    }
}
