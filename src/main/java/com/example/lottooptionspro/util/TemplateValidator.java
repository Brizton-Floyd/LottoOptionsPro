package com.example.lottooptionspro.util;

import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.models.PanelValidationResult;
import com.example.lottooptionspro.models.TemplateValidationResult;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core validation engine for template validation
 */
public class TemplateValidator {
    private static final int DEFAULT_MAX_TESTS = 100;
    private static final int DEFAULT_NUMBER_RANGE_START = 1;
    private static final int DEFAULT_NUMBER_RANGE_END = 54; // Common for Texas Lotto
    
    private final int maxTests;
    private final int numberRangeStart;
    private final int numberRangeEnd;
    
    public TemplateValidator() {
        this(DEFAULT_MAX_TESTS, DEFAULT_NUMBER_RANGE_START, DEFAULT_NUMBER_RANGE_END);
    }
    
    public TemplateValidator(int maxTests, int numberRangeStart, int numberRangeEnd) {
        this.maxTests = maxTests;
        this.numberRangeStart = numberRangeStart;
        this.numberRangeEnd = numberRangeEnd;
    }
    
    /**
     * Validate a template with multiple panels
     */
    public TemplateValidationResult validateTemplate(Map<String, Map<String, Coordinate>> panelCoordinates) {
        long startTime = System.currentTimeMillis();
        TemplateValidationResult result = new TemplateValidationResult();
        
        // Initialize results for all panels
        for (String panelId : Arrays.asList("A", "B", "C", "D", "E")) {
            PanelValidationResult panelResult = new PanelValidationResult(panelId);
            Map<String, Coordinate> coordinates = panelCoordinates.get(panelId);
            
            if (coordinates != null && !coordinates.isEmpty()) {
                panelResult.setCoordinatesMapped(coordinates.size());
                panelResult.setExpectedNumbers(numberRangeEnd - numberRangeStart + 1);
                validatePanel(panelResult, coordinates);
            }
            
            result.addPanelResult(panelResult);
        }
        
        result.setTotalTestsRun(maxTests);
        result.calculateOverallStatus();
        result.setValidationTimeMs(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * Validate a single panel
     */
    private void validatePanel(PanelValidationResult result, Map<String, Coordinate> coordinates) {
        Set<String> availableNumbers = coordinates.keySet();
        int testsCompleted = 0;
        
        // Run validation tests
        for (int test = 0; test < maxTests; test++) {
            List<String> randomNumbers = generateRandomNumbers(availableNumbers);
            
            boolean testPassed = true;
            for (String number : randomNumbers) {
                Coordinate coord = coordinates.get(number);
                if (coord == null) {
                    result.addError("Test " + (test + 1) + ": Missing coordinate for number " + number);
                    testPassed = false;
                } else if (!isValidCoordinate(coord)) {
                    result.addError("Test " + (test + 1) + ": Invalid coordinate for number " + number + 
                                  " (" + coord.getX() + ", " + coord.getY() + ")");
                    testPassed = false;
                } else {
                    result.addTestedNumber(number);
                }
            }
            
            if (testPassed) {
                testsCompleted++;
            }
        }
        
        result.setTestsRun(testsCompleted);
        result.updateCoverage();
        
        // Consider panel passed if most tests succeeded and no critical errors
        double passRate = (double) testsCompleted / maxTests;
        result.setPassed(passRate >= 0.95 && result.getErrors().isEmpty());
    }
    
    /**
     * Generate random numbers for testing
     */
    private List<String> generateRandomNumbers(Set<String> availableNumbers) {
        List<String> numbers = new ArrayList<>(availableNumbers);
        Collections.shuffle(numbers);
        
        // Select 6 random numbers (typical lottery selection)
        int selectionSize = Math.min(6, numbers.size());
        return numbers.subList(0, selectionSize);
    }
    
    /**
     * Validate that a coordinate is within reasonable bounds
     */
    private boolean isValidCoordinate(Coordinate coord) {
        // Basic validation - coordinates should be positive
        return coord.getX() > 0 && coord.getY() > 0 && 
               coord.getX() < 10000 && coord.getY() < 10000; // Reasonable upper bounds
    }
    
    /**
     * Generate random numbers within the configured range
     */
    public List<Integer> generateRandomNumbersInRange(int count) {
        Set<Integer> numbers = new HashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        while (numbers.size() < count && numbers.size() < (numberRangeEnd - numberRangeStart + 1)) {
            numbers.add(random.nextInt(numberRangeStart, numberRangeEnd + 1));
        }
        
        return new ArrayList<>(numbers);
    }
    
    // Getters
    public int getMaxTests() {
        return maxTests;
    }
    
    public int getNumberRangeStart() {
        return numberRangeStart;
    }
    
    public int getNumberRangeEnd() {
        return numberRangeEnd;
    }
}