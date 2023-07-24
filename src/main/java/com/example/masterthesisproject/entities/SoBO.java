package com.example.masterthesisproject.entities;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;
public class SoBO {
    private String id;
    private Map<String, Object> properties = new HashMap<>();

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        addProperty(name, value);
    }
}
