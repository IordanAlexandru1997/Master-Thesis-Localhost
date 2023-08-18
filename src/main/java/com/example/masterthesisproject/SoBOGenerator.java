package com.example.masterthesisproject;


import com.example.masterthesisproject.entities.Edge;
import com.example.masterthesisproject.entities.SoBO;
import com.orientechnologies.orient.core.record.OVertex;

import java.util.*;

public class SoBOGenerator {
    public static final List<String> GENERATED_SoBO_IDs = SoBOIdTracker.loadSoBOIds(); // Load existing SoBO IDs

    private static final List<String> KEY_NAMES = Arrays.asList("name", "email", "age", "outlook", "yahoo");
    private static final List<String> EDGE_TYPES = Arrays.asList("RELATED_TO", "FRIENDS_WITH", "WORKS_WITH");
    private static final Random RANDOM = new Random();
    public static final List<SoBO> GENERATED_SoBOs = new ArrayList<>();

    public static SoBO generateRandomSoBO() {
        // here we generate a random number of keys for the SoBO object.
        // The keys are chosen from the KEY_NAMES list
        // The values are random
        // The SoBO object is added to the GENERATED_SoBOs list.
        List<String> idKeys = new ArrayList<>(KEY_NAMES.subList(0, RANDOM.nextInt(KEY_NAMES.size()) + 1));
        SoBO sobo = new SoBO(idKeys);

        for (String key : idKeys) {
            Object value = getRandomValue();
            sobo.addProperty(key, value);
        }

        sobo.addProperty("age", RANDOM.nextInt(100));
        sobo.addProperty("isActive", RANDOM.nextBoolean());

        GENERATED_SoBOs.add(sobo);
        GENERATED_SoBO_IDs.add(sobo.getId());
//        SoBOIdTracker.saveSoBOIds(GENERATED_SoBO_IDs); // Save the updated SoBO IDs list to the file



        return sobo;
    }
    private static Object getRandomValue() {
        int type = RANDOM.nextInt(2);
        switch (type) {
            case 0:
                return RANDOM.nextInt(1000);
            case 1:
                return "xyz@gmail.com";
        }
        return null;
    }


    public static Edge generateRandomEdge() {
        if (GENERATED_SoBOs.size() < 2) {
            throw new IllegalStateException("At least two SoBO objects must be created before generating an edge");
        }

        SoBO sobo1 = getRandomSoBO();
        SoBO sobo2 = null;
        do {
            sobo2 = getRandomSoBO();
        } while (sobo2.getId().equals(sobo1.getId())); // the two SoBOs are different

        String edgeType = EDGE_TYPES.get(RANDOM.nextInt(EDGE_TYPES.size()));

        return new Edge(sobo1, sobo2, edgeType);
    }
    public static List<Edge> generateEdgesForSoBOs(List<SoBO> sobos, int minEdges, int maxEdges) {
        int totalSoBOs = sobos.size();
        int maxPossibleEdges = totalSoBOs * (totalSoBOs - 1) / 2;
        List<Edge> edges = new ArrayList<>();

        for (SoBO sobo : sobos) {
            int edgeCount = new Random().nextInt((maxEdges - minEdges) + 1) + minEdges;
            for (int i = 0; i < edgeCount && edges.size() < maxPossibleEdges; i++) {
                SoBO targetSoBO;
                do {
                    targetSoBO = getRandomSoBO();
                } while (targetSoBO.getId().equals(sobo.getId()) || edges.contains(new Edge(sobo, targetSoBO, "")));

                Edge edge = new Edge(sobo, targetSoBO, EDGE_TYPES.get(RANDOM.nextInt(EDGE_TYPES.size())));
                edges.add(edge);
            }
        }
        return edges;
    }


    public static SoBO getRandomSoBO() {
        if (GENERATED_SoBOs.isEmpty()) {
            System.out.println("No SoBOs have been generated yet. Generating one now...");
            return generateRandomSoBO();
        }
        return GENERATED_SoBOs.get(RANDOM.nextInt(GENERATED_SoBOs.size()));
    }
    public static String getRandomSoBOId() {
        if (GENERATED_SoBO_IDs.isEmpty()) {
            System.err.println("No SoBOs have been generated. Cannot fetch a random SoBO ID.");
            return null; // or throw an exception, depending on your use case
        }
        return GENERATED_SoBO_IDs.get(RANDOM.nextInt(GENERATED_SoBO_IDs.size()));
    }

    public static void removeSoBO(SoBO sobo) {
        GENERATED_SoBO_IDs.remove(sobo.getId());
        GENERATED_SoBOs.remove(sobo);
        SoBOIdTracker.saveSoBOIds(GENERATED_SoBO_IDs); // Save the updated SoBO IDs list to the file

    }

}