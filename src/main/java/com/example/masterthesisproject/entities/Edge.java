package com.example.masterthesisproject.entities;

import java.util.HashMap;
import java.util.Map;

public class Edge {
    private SoBO soboObj1;
    private SoBO soboObj2;
    private String type;
    private Map<String, Object> properties;

    public Edge(SoBO soboObj1, SoBO soboObj2, String type, Map<String, Object> properties) {
        this.soboObj1 = soboObj1;
        this.soboObj2 = soboObj2;
        this.type = type;
        this.properties = properties;
    }
    public Edge(SoBO soboObj1, SoBO soboObj2, String type) {
        this.soboObj1 = soboObj1;
        this.soboObj2 = soboObj2;
        this.type = type;
        this.properties= new HashMap<>();
    }
    public SoBO getSoboObj1() {
        return soboObj1;
    }

    public SoBO getSoboObj2() {
        return soboObj2;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
