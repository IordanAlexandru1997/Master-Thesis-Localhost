package com.example.masterthesisproject.entities;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public class SoBO {
    private String id;
    private Map<String, Object> properties = new HashMap<>();
    private List<String> idKeys;

    public SoBO(String id) {
        this.id = id;
        this.properties.put("id", id);
    }
    public SoBO(){}
    public void setId(String id) {
        this.id = id;
    }

    public SoBO(List<String> idKeys) {
        this.idKeys = idKeys;
        updateId();
    }

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
        if (idKeys.contains(key)) {
            updateId();
        }
    }

    private void updateId() {
        StringBuilder idBuilder = new StringBuilder();
        for (String key : idKeys) {
            idBuilder.append(properties.get(key));
        }

        String shortUUID = UUID.randomUUID().toString().substring(0, 8);
        idBuilder.append(shortUUID);

        this.id = idBuilder.toString();
        properties.put("id", this.id);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

}
