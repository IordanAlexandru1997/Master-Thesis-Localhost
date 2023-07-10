package com.example.masterthesisproject.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DockerContainerChecker {

    public static boolean isContainerRunning(String containerName) {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "docker ps | findstr " + containerName);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(containerName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
