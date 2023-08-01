package com.example.masterthesisproject.entities;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class SoBO {
    private String id;
    private Map<String, Object> properties = new HashMap<>();
    private List<String> idKeys;

    public SoBO() {
    }

    public SoBO(List<String> idKeys) {
        this.idKeys = idKeys;
    }

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
        if (idKeys.contains(key)) {
            updateId();
        }
    }

    private void updateId() {
        StringBuilder newId = new StringBuilder();
        for (String key : idKeys) {
            if (properties.containsKey(key)) {
                newId.append(properties.get(key));
            }
        }
        setId(newId.toString());
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.properties.put("id", id);
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        addProperty(name, value);
    }
}

