package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class WheelPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(WheelPersistenceService.class);
    private final WheelVerificationService verificationService;
    
    public WheelPersistenceService(WheelVerificationService verificationService) {
        this.verificationService = verificationService;
    }
    
    public boolean saveWheel(WheelParameters params, List<int[]> wheel) {
        if (!verifyWheelBeforeSave(params, wheel)) {
            logger.error("Wheel failed verification, not saving");
            return false;
        }
        
        try {
            Path currentPath = Paths.get("").toAbsolutePath();
            Path wheelsRoot;
            
            if (currentPath.toString().contains("src/main/resources")) {
                wheelsRoot = currentPath.getParent().getParent().getParent().resolve("wheels");
            } else if (currentPath.toString().endsWith("lottooptionspro")) {
                wheelsRoot = currentPath.resolve("wheels");
            } else {
                wheelsRoot = Paths.get("wheels").toAbsolutePath();
            }
            
            Path dir = wheelsRoot.resolve("pick" + params.getK()).resolve(String.valueOf(params.getV()));
            Files.createDirectories(dir);
            
            String filename = params.getM() + "_if_" + params.getT() + ".txt";
            Path file = dir.resolve(filename);
            
            String content = formatWheelForFile(wheel);
            Files.writeString(file, content);
            
            logger.info("Saved wheel to: {} ({} lines)", file.toAbsolutePath(), wheel.size());
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to save wheel: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private String formatWheelForFile(List<int[]> wheel) {
        StringBuilder content = new StringBuilder();
        
        for (int[] ticket : wheel) {
            for (int i = 0; i < ticket.length; i++) {
                int num = ticket[i] + 1;
                
                if (i > 0) {
                    content.append("  ");
                }
                
                if (num < 10) {
                    content.append(" ").append(num);
                } else {
                    content.append(num);
                }
            }
            content.append("\n");
        }
        
        return content.toString();
    }
    
    private boolean verifyWheelBeforeSave(WheelParameters params, List<int[]> wheel) {
        logger.info("Verifying wheel before save: {}", params.getNotation());
        
        Set<Set<Integer>> universe = generateUniverse(params);
        Set<Set<Integer>> covered = computeCoveredElements(wheel, universe, params.getM());
        
        if (covered.size() != universe.size()) {
            logger.error("Wheel does not provide 100% coverage! Covered {}/{}", 
                        covered.size(), universe.size());
            return false;
        }
        
        logger.info("Wheel verified: 100% coverage ({} combinations)", universe.size());
        
        if (wheel.size() < 100 && !verifyMinimality(wheel, universe, params.getM())) {
            logger.warn("Wheel contains redundant tickets but will save anyway");
        } else if (wheel.size() >= 100) {
            logger.info("Skipping minimality check for large wheel ({} lines)", wheel.size());
        }
        
        int lowerBound = computeLowerBound(params);
        double efficiency = 100.0 * (1.0 - (double)wheel.size() / lowerBound);
        logger.info("Wheel efficiency: {} lines vs {} naive lower bound ({}% reduction)",
                   wheel.size(), lowerBound, String.format("%.1f", efficiency));
        
        return true;
    }
    
    private boolean verifyMinimality(List<int[]> wheel, Set<Set<Integer>> universe, int m) {
        for (int i = 0; i < wheel.size(); i++) {
            List<int[]> withoutTicket = new ArrayList<>(wheel);
            withoutTicket.remove(i);
            
            Set<Set<Integer>> coveredWithout = computeCoveredElements(withoutTicket, universe, m);
            if (coveredWithout.size() == universe.size()) {
                logger.warn("Ticket {} is redundant - wheel not minimal", i);
                return false;
            }
        }
        
        logger.info("Wheel verified as minimal: no redundant tickets");
        return true;
    }
    
    private Set<Set<Integer>> generateUniverse(WheelParameters params) {
        Set<Set<Integer>> universe = new HashSet<>();
        
        List<int[]> tCombos = generateAllCombinations(params.getV(), params.getT());
        for (int[] tCombo : tCombos) {
            universe.add(arrayToSet(tCombo));
        }
        
        return universe;
    }
    
    private Set<Set<Integer>> computeCoveredElements(List<int[]> wheel, Set<Set<Integer>> universe, int m) {
        Set<Set<Integer>> covered = new HashSet<>();
        
        for (int[] ticket : wheel) {
            for (Set<Integer> draw : universe) {
                if (countMatches(ticket, draw) >= m) {
                    covered.add(draw);
                }
            }
        }
        
        return covered;
    }
    
    private int countMatches(int[] ticket, Set<Integer> draw) {
        int matches = 0;
        for (int num : ticket) {
            if (draw.contains(num)) {
                matches++;
            }
        }
        return matches;
    }
    
    private int computeLowerBound(WheelParameters params) {
        int universeSize;
        if (params.getT() == params.getM()) {
            universeSize = binomial(params.getV(), params.getM());
        } else {
            universeSize = binomial(params.getV(), params.getT()) * binomial(params.getT(), params.getM());
        }
        
        int coveragePerTicket = binomial(params.getK(), params.getM());
        
        return (int) Math.ceil((double) universeSize / coveragePerTicket);
    }
    
    private List<int[]> generateAllCombinations(int n, int k) {
        List<int[]> result = new ArrayList<>();
        int[] combo = new int[k];
        generateCombinationsRecursive(n, k, 0, 0, combo, result);
        return result;
    }
    
    private void generateCombinationsRecursive(int n, int k, int start, int index, int[] combo, List<int[]> result) {
        if (index == k) {
            result.add(combo.clone());
            return;
        }
        
        for (int i = start; i <= n - k + index; i++) {
            combo[index] = i;
            generateCombinationsRecursive(n, k, i + 1, index + 1, combo, result);
        }
    }
    
    private List<int[]> generateCombinationsFromArray(int[] array, int k) {
        List<int[]> result = new ArrayList<>();
        int[] combo = new int[k];
        generateFromArrayRecursive(array, k, 0, 0, combo, result);
        return result;
    }
    
    private void generateFromArrayRecursive(int[] array, int k, int start, int index, int[] combo, List<int[]> result) {
        if (index == k) {
            result.add(combo.clone());
            return;
        }
        
        for (int i = start; i <= array.length - k + index; i++) {
            combo[index] = array[i];
            generateFromArrayRecursive(array, k, i + 1, index + 1, combo, result);
        }
    }
    
    private Set<Integer> arrayToSet(int[] array) {
        Set<Integer> set = new HashSet<>();
        for (int num : array) {
            set.add(num);
        }
        return set;
    }
    
    private int binomial(int n, int k) {
        if (k > n - k) {
            k = n - k;
        }
        
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        
        return (int) result;
    }
}
