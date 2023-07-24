package com.example.masterthesisproject.entities;

import java.util.Map;

public class CreateRelationshipRequest {
    private Map<String, Object> soboObj1;
    private Map<String, Object> soboObj2;
    private String matchField;
    private String soboLabel1;
    private String soboLabel2;

    // constructors, getters and setters...

    public CreateRelationshipRequest() {
    }

    public CreateRelationshipRequest(Map<String, Object> soboObj1, Map<String, Object> soboObj2, String matchField, String soboLabel1, String soboLabel2) {
        this.soboObj1 = soboObj1;
        this.soboObj2 = soboObj2;
        this.matchField = matchField;
        this.soboLabel1 = soboLabel1;
        this.soboLabel2 = soboLabel2;
    }

    public Map<String, Object> getSoboObj1() {
        return soboObj1;
    }

    public void setSoboObj1(Map<String, Object> soboObj1) {
        this.soboObj1 = soboObj1;
    }

    public Map<String, Object> getSoboObj2() {
        return soboObj2;
    }

    public void setSoboObj2(Map<String, Object> soboObj2) {
        this.soboObj2 = soboObj2;
    }

    public String getMatchField() {
        return matchField;
    }

    public void setMatchField(String matchField) {
        this.matchField = matchField;
    }

    public String getSoboLabel1() {
        return soboLabel1;
    }

    public void setSoboLabel1(String soboLabel1) {
        this.soboLabel1 = soboLabel1;
    }

    public String getSoboLabel2() {
        return soboLabel2;
    }

    public void setSoboLabel2(String soboLabel2) {
        this.soboLabel2 = soboLabel2;
    }
}