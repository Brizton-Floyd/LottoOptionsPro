package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WheelVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WheelVerificationService.class);
    
    public VerificationResult verify(List<int[]> wheel, WheelParameters params) {
        logger.info("Verifying wheel: {} lines for {}", wheel.size(), params.getNotation());
        
        int totalDraws = binomial(params.getV(), params.getT());
        int passedDraws = 0;
        int failedDraws = 0;
        List<int[]> failedCases = new ArrayList<>();
        
        List<int[]> allDraws = generateAllCombinations(params.getV(), params.getT());
        
        for (int[] draw : allDraws) {
            boolean hasMatch = checkDrawAgainstWheel(draw, wheel, params.getM());
            
            if (hasMatch) {
                passedDraws++;
            } else {
                failedDraws++;
                if (failedCases.size() < 10) {
                    failedCases.add(draw);
                }
            }
        }
        
        boolean verified = failedDraws == 0;
        double coverage = (passedDraws * 100.0) / totalDraws;
        
        VerificationResult result = new VerificationResult();
        result.setVerified(verified);
        result.setTotalDraws(totalDraws);
        result.setPassedDraws(passedDraws);
        result.setFailedDraws(failedDraws);
        result.setCoveragePercentage(coverage);
        result.setFailedCases(failedCases);
        
        logger.info("Verification result: {} ({}/{} draws passed, {}% coverage)",
                   verified ? "PASS" : "FAIL", passedDraws, totalDraws, String.format("%.2f", coverage));
        
        if (!verified && failedCases.size() > 0) {
            logger.warn("First failed case: {}", Arrays.toString(failedCases.get(0)));
        }
        
        return result;
    }
    
    private boolean checkDrawAgainstWheel(int[] draw, List<int[]> wheel, int requiredMatches) {
        Set<Integer> drawSet = new HashSet<>();
        for (int num : draw) {
            drawSet.add(num);
        }
        
        for (int[] ticket : wheel) {
            int matches = 0;
            for (int num : ticket) {
                if (drawSet.contains(num)) {
                    matches++;
                }
            }
            
            if (matches >= requiredMatches) {
                return true;
            }
        }
        
        return false;
    }
    
    public List<int[]> mapIndicesToNumbers(List<int[]> wheel, List<Integer> userNumbers) {
        List<int[]> mappedWheel = new ArrayList<>();
        
        for (int[] ticket : wheel) {
            int[] mappedTicket = new int[ticket.length];
            for (int i = 0; i < ticket.length; i++) {
                mappedTicket[i] = userNumbers.get(ticket[i]);
            }
            Arrays.sort(mappedTicket);
            mappedWheel.add(mappedTicket);
        }
        
        return mappedWheel;
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
    
    public static class VerificationResult {
        private boolean verified;
        private int totalDraws;
        private int passedDraws;
        private int failedDraws;
        private double coveragePercentage;
        private List<int[]> failedCases;
        
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        
        public int getTotalDraws() { return totalDraws; }
        public void setTotalDraws(int totalDraws) { this.totalDraws = totalDraws; }
        
        public int getPassedDraws() { return passedDraws; }
        public void setPassedDraws(int passedDraws) { this.passedDraws = passedDraws; }
        
        public int getFailedDraws() { return failedDraws; }
        public void setFailedDraws(int failedDraws) { this.failedDraws = failedDraws; }
        
        public double getCoveragePercentage() { return coveragePercentage; }
        public void setCoveragePercentage(double coveragePercentage) { this.coveragePercentage = coveragePercentage; }
        
        public List<int[]> getFailedCases() { return failedCases; }
        public void setFailedCases(List<int[]> failedCases) { this.failedCases = failedCases; }
    }
}
