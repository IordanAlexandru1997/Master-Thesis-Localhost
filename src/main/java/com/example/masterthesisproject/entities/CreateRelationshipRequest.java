package com.example.masterthesisproject.entities;

import java.util.Map;
public class CreateRelationshipRequest {
    private Edge edge;
    private String soboLabel1;
    private String soboLabel2;

    public CreateRelationshipRequest(Edge edge, String soboLabel1, String soboLabel2) {
        this.edge = edge;
        this.soboLabel1 = soboLabel1;
        this.soboLabel2 = soboLabel2;
    }

    public Edge getEdge() {
        return edge;
    }

    public String getSoboLabel1() {
        return soboLabel1;
    }

    public String getSoboLabel2() {
        return soboLabel2;
    }
}

