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

    public static void initializeCreation() {
        GENERATED_SoBOs.clear();
        GENERATED_SoBO_IDs.clear();
    }


}