package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatternBasedWheelGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(PatternBasedWheelGenerator.class);
    private final Map<String, List<int[]>> templateWheels = new HashMap<>();
    
    public PatternBasedWheelGenerator() {
        loadTemplateWheels();
    }
    
    private void loadTemplateWheels() {
        try {
            Path wheelsDir = Paths.get("wheels");
            if (!Files.exists(wheelsDir)) {
                logger.warn("Wheels directory not found at: {}", wheelsDir.toAbsolutePath());
                return;
            }
            
            logger.info("Loading template wheels from: {}", wheelsDir.toAbsolutePath());
            
            Files.walk(wheelsDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(this::loadTemplateWheel);
            
            logger.info("Loaded {} template wheels", templateWheels.size());
        } catch (IOException e) {
            logger.error("Failed to load template wheels", e);
        }
    }
    
    private void loadTemplateWheel(Path file) {
        try {
            String path = file.toString();
            
            String pickSizeStr = path.contains("pick5") ? "5" : path.contains("pick6") ? "6" : "5";
            int pickSize = Integer.parseInt(pickSizeStr);
            
            String poolSizeStr = file.getParent().getFileName().toString();
            int poolSize = Integer.parseInt(poolSizeStr);
            
            String filename = file.getFileName().toString().replace(".txt", "");
            String[] parts = filename.split("_if_");
            int m = Integer.parseInt(parts[0]);
            int t = Integer.parseInt(parts[1]);
            
            String key = String.format("%d-%d-%d-%d", poolSize, pickSize, t, m);
            
            List<int[]> wheel = new ArrayList<>();
            List<String> lines = Files.readAllLines(file);
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] nums = line.split("\\s+");
                int[] combo = new int[nums.length];
                for (int i = 0; i < nums.length; i++) {
                    combo[i] = Integer.parseInt(nums[i]) - 1;
                }
                Arrays.sort(combo);
                wheel.add(combo);
            }
            
            templateWheels.put(key, wheel);
            logger.info("Loaded template wheel: {} with {} lines", key, wheel.size());
            
        } catch (Exception e) {
            logger.error("Failed to load template wheel: {}", file, e);
        }
    }
    
    public List<int[]> generateFromTemplate(WheelParameters params) {
        String key = String.format("%d-%d-%d-%d", params.getV(), params.getK(), params.getT(), params.getM());
        
        List<int[]> template = templateWheels.get(key);
        if (template != null) {
            logger.info("Using exact template match for {}", key);
            return new ArrayList<>(template);
        }
        
        List<int[]> scaledWheel = tryScaleFromSimilarWheel(params);
        if (scaledWheel != null) {
            logger.info("Generated wheel by scaling from similar template");
            return scaledWheel;
        }
        
        return null;
    }
    
    private List<int[]> tryScaleFromSimilarWheel(WheelParameters params) {
        for (Map.Entry<String, List<int[]>> entry : templateWheels.entrySet()) {
            String[] keyParts = entry.getKey().split("-");
            int templateV = Integer.parseInt(keyParts[0]);
            int templateK = Integer.parseInt(keyParts[1]);
            int templateT = Integer.parseInt(keyParts[2]);
            int templateM = Integer.parseInt(keyParts[3]);
            
            if (templateK == params.getK() && templateT == params.getT() && templateM == params.getM()) {
                if (templateV < params.getV()) {
                    return scaleWheelUp(entry.getValue(), templateV, params.getV(), params.getK());
                }
            }
        }
        
        return null;
    }
    
    private List<int[]> scaleWheelUp(List<int[]> template, int fromSize, int toSize, int k) {
        if (toSize <= fromSize) return null;
        
        List<int[]> scaled = new ArrayList<>();
        int additionalNumbers = toSize - fromSize;
        
        for (int[] combo : template) {
            scaled.add(combo.clone());
        }
        
        Set<Set<Integer>> coveredSubsets = new HashSet<>();
        for (int[] combo : scaled) {
            for (int i = 0; i < combo.length; i++) {
                for (int j = i + 1; j < combo.length; j++) {
                    for (int l = j + 1; l < combo.length; l++) {
                        Set<Integer> subset = new HashSet<>();
                        subset.add(combo[i]);
                        subset.add(combo[j]);
                        subset.add(combo[l]);
                        coveredSubsets.add(subset);
                    }
                }
            }
        }
        
        Random rand = new Random(42);
        for (int newNum = fromSize; newNum < toSize; newNum++) {
            List<Integer> existingNums = new ArrayList<>();
            for (int i = 0; i < fromSize; i++) {
                existingNums.add(i);
            }
            Collections.shuffle(existingNums, rand);
            
            int[] newCombo = new int[k];
            newCombo[0] = newNum;
            for (int i = 1; i < k; i++) {
                newCombo[i] = existingNums.get(i - 1);
            }
            Arrays.sort(newCombo);
            scaled.add(newCombo);
        }
        
        logger.info("Scaled wheel from {} to {} numbers, {} -> {} lines", 
                   fromSize, toSize, template.size(), scaled.size());
        
        return scaled;
    }
    
    public boolean hasTemplate(WheelParameters params) {
        String key = String.format("%d-%d-%d-%d", params.getV(), params.getK(), params.getT(), params.getM());
        return templateWheels.containsKey(key);
    }
    
    public Set<String> getAvailableTemplates() {
        return new HashSet<>(templateWheels.keySet());
    }
    
    public List<Integer> getAvailablePoolSizes(int pickSize) {
        Set<Integer> poolSizes = new HashSet<>();
        for (String key : templateWheels.keySet()) {
            String[] parts = key.split("-");
            if (parts.length >= 2) {
                int v = Integer.parseInt(parts[0]);
                int k = Integer.parseInt(parts[1]);
                if (k == pickSize) {
                    poolSizes.add(v);
                }
            }
        }
        List<Integer> sorted = new ArrayList<>(poolSizes);
        Collections.sort(sorted);
        return sorted;
    }
    
    public List<String> getAvailableGuarantees(int poolSize, int pickSize) {
        Set<String> guarantees = new HashSet<>();
        for (String key : templateWheels.keySet()) {
            String[] parts = key.split("-");
            if (parts.length >= 4) {
                int v = Integer.parseInt(parts[0]);
                int k = Integer.parseInt(parts[1]);
                int t = Integer.parseInt(parts[2]);
                int m = Integer.parseInt(parts[3]);
                if (v == poolSize && k == pickSize) {
                    guarantees.add(String.format("%d-%d-%d-%d", v, k, t, m));
                }
            }
        }
        return new ArrayList<>(guarantees);
    }
    
    public int getWheelLineCount(int poolSize, int pickSize, int t, int m) {
        String key = String.format("%d-%d-%d-%d", poolSize, pickSize, t, m);
        List<int[]> wheel = templateWheels.get(key);
        return wheel != null ? wheel.size() : 0;
    }
}
