package com.example.masterthesisproject;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoBOIdTracker {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FILE_NAME = "sobo_obj.json";

    public static List<String> loadSoBOIds() {
        try {
            File file = new File(FILE_NAME);
            if (file.exists()) {
                List<String> soboIds = objectMapper.readValue(file, new TypeReference<>() {});
//                System.out.println("Loaded SoBO IDs from " + FILE_NAME + ": " + soboIds); // Debugging print
                return soboIds;
            } else {
                System.out.println("No SoBO IDs found in " + FILE_NAME);
                return new ArrayList<>();
            }
        } catch (IOException e) {
            System.err.println("Error loading SoBO IDs from " + FILE_NAME + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public static void clearSoBOFile() {
        try {
            FileWriter writer = new FileWriter(FILE_NAME, false);
            writer.write(""); // Clearing the content
            writer.close();
            System.out.println("SoBO file cleared");
        } catch (IOException e) {
            System.err.println("Error clearing SoBO file: " + e.getMessage());
        }
    }

    public static void saveSoBOIds(List<String> soboIds) {
        try {
            objectMapper.writeValue(new File(FILE_NAME), soboIds);
            System.out.println("SoBO IDs saved to " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Error saving SoBO IDs to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}