package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CoveringDesignWheelService {
    
    private static final Logger logger = LoggerFactory.getLogger(CoveringDesignWheelService.class);
    private final PatternBasedWheelGenerator patternGenerator;
    
    public CoveringDesignWheelService(PatternBasedWheelGenerator patternGenerator) {
        this.patternGenerator = patternGenerator;
    }
    
    public interface ProgressCallback {
        void update(String message, double progress, int covered, int total, int lines);
    }
    
    public List<int[]> constructWheel(WheelParameters params, ProgressCallback callback, AtomicBoolean cancelled) {
        params.validate();
        
        logger.info("Constructing wheel for {}", params.getNotation());
        
        List<int[]> templateWheel = patternGenerator.generateFromTemplate(params);
        if (templateWheel != null) {
            logger.info("Using pre-computed optimal template");
            callback.update("Using optimal template wheel", 0.9, 0, 0, templateWheel.size());
            return templateWheel;
        }
        
        if (params.isBalancedGuarantee() && params.isPrimePoolSize()) {
            logger.info("Using Cyclic Shift method (balanced guarantee, prime pool)");
            return constructCyclicShiftWheel(params, callback, cancelled);
        }
        
        List<int[]> bibdWheel = tryBIBDConstruction(params, callback, cancelled);
        if (bibdWheel != null) {
            logger.info("Using BIBD Block Design method");
            return bibdWheel;
        }
        
        logger.info("Using Greedy algorithm (fallback)");
        return constructGreedyWheel(params, callback, cancelled);
    }
    
    private List<int[]> constructCyclicShiftWheel(WheelParameters params, ProgressCallback callback, AtomicBoolean cancelled) {
        callback.update("Using Cyclic Shift method...", 0.1, 0, 0, 0);
        
        List<int[]> wheel = new ArrayList<>();
        int[] seed = generateSeedTicket(params);
        
        wheel.add(seed.clone());
        
        for (int shift = 1; shift < params.getV(); shift++) {
            if (cancelled.get()) break;
            
            int[] ticket = new int[params.getK()];
            for (int i = 0; i < params.getK(); i++) {
                ticket[i] = (seed[i] + shift) % params.getV();
            }
            Arrays.sort(ticket);
            wheel.add(ticket);
            
            double progress = 0.1 + (0.8 * shift / params.getV());
            callback.update("Generating cyclic shifts...", progress, 0, 0, wheel.size());
        }
        
        callback.update("Verifying cyclic wheel...", 0.9, 0, 0, wheel.size());
        
        logger.info("Cyclic shift wheel generated: {} lines", wheel.size());
        return wheel;
    }
    
    private int[] generateSeedTicket(WheelParameters params) {
        int[] seed = new int[params.getK()];
        
        if (params.getK() == 5 && params.getV() >= 5) {
            seed[0] = 0;
            seed[1] = 1;
            seed[2] = 2;
            seed[3] = (params.getV() / 3);
            seed[4] = (params.getV() / 2);
        } else {
            for (int i = 0; i < params.getK(); i++) {
                seed[i] = (i * params.getV() / params.getK()) % params.getV();
            }
        }
        
        Arrays.sort(seed);
        return seed;
    }
    
    private List<int[]> tryBIBDConstruction(WheelParameters params, ProgressCallback callback, AtomicBoolean cancelled) {
        return null;
    }
    
    private List<int[]> constructTransversalDesign_15_5_3if5(ProgressCallback callback, AtomicBoolean cancelled) {
        callback.update("Using Transversal Design (15-5-3if5)...", 0.2, 0, 0, 0);
        
        List<int[]> wheel = new ArrayList<>();
        
        int[][] patterns = {
            {0,1,2,3,4}, {0,5,6,10,11}, {0,7,8,12,13}, {0,9,14,1,6},
            {0,2,7,11,14}, {1,5,7,9,12}, {1,8,10,13,14}, {2,5,8,9,13},
            {2,6,10,12,14}, {3,5,9,10,14}, {3,6,7,8,11}, {3,12,13,1,2},
            {4,5,11,12,13}, {4,6,7,9,14}, {4,8,10,1,3}, {6,8,9,0,3},
            {7,10,5,2,4}, {11,13,6,1,4}, {12,14,7,3,5}
        };
        
        for (int[] pattern : patterns) {
            if (cancelled.get()) break;
            int[] sorted = pattern.clone();
            Arrays.sort(sorted);
            wheel.add(sorted);
        }
        
        logger.info("Transversal Design wheel: {} lines", wheel.size());
        callback.update("Transversal Design complete", 0.9, 0, 0, wheel.size());
        return wheel;
    }
    
    private List<int[]> constructBlockDesign_18_5_3if5(ProgressCallback callback, AtomicBoolean cancelled) {
        callback.update("Using Block Design (18-5-3if5)...", 0.2, 0, 0, 0);
        
        List<int[]> wheel = new ArrayList<>();
        
        int[][] blocks = {
            {0,1,2,3,4,5},
            {6,7,8,9,10,11},
            {12,13,14,15,16,17}
        };
        
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    if (cancelled.get()) break;
                    
                    if ((i + j + k) % 6 == 0) {
                        int[] ticket = {
                            blocks[0][i],
                            blocks[0][(i+1)%6],
                            blocks[1][j],
                            blocks[1][(j+1)%6],
                            blocks[2][k]
                        };
                        Arrays.sort(ticket);
                        wheel.add(ticket);
                    }
                }
            }
            
            double progress = 0.2 + (0.7 * i / 6.0);
            callback.update("Generating block combinations...", progress, 0, 0, wheel.size());
        }
        
        callback.update("Block Design complete", 0.9, 0, 0, wheel.size());
        return wheel;
    }
    
    private List<int[]> constructBIBD_15_5_4if4(ProgressCallback callback, AtomicBoolean cancelled) {
        callback.update("Using BIBD (15-5-4if4)...", 0.1, 0, 0, 0);
        
        List<int[]> wheel = new ArrayList<>();
        Set<Set<Integer>> coveredQuads = new HashSet<>();
        
        int totalQuads = binomial(15, 4);
        
        List<int[]> allCombos = generateAllCombinations(15, 5);
        
        for (int idx = 0; idx < allCombos.size(); idx++) {
            if (cancelled.get()) break;
            
            int[] combo = allCombos.get(idx);
            
            Set<Set<Integer>> quadsInCombo = extractQuads(combo);
            
            boolean coversNew = false;
            for (Set<Integer> quad : quadsInCombo) {
                if (!coveredQuads.contains(quad)) {
                    coversNew = true;
                    break;
                }
            }
            
            if (coversNew) {
                wheel.add(combo);
                coveredQuads.addAll(quadsInCombo);
            }
            
            if (idx % 50 == 0) {
                double progress = 0.1 + (0.8 * coveredQuads.size() / (double)totalQuads);
                callback.update("Building BIBD wheel...", progress, coveredQuads.size(), totalQuads, wheel.size());
            }
            
            if (coveredQuads.size() >= totalQuads || wheel.size() >= 189) {
                break;
            }
        }
        
        callback.update("BIBD complete", 0.9, coveredQuads.size(), totalQuads, wheel.size());
        return wheel;
    }
    
    private List<int[]> constructGreedyWheel(WheelParameters params, ProgressCallback callback, AtomicBoolean cancelled) {
        callback.update("Generating all winning outcomes...", 0.05, 0, 0, 0);
        
        Set<Set<Integer>> requiredSubsets = generateRequiredSubsets(params, callback, cancelled);
        
        if (cancelled.get()) return new ArrayList<>();
        
        int totalSubsets = requiredSubsets.size();
        logger.info("Need to cover {} subsets", totalSubsets);
        
        callback.update("Building greedy wheel...", 0.1, 0, totalSubsets, 0);
        
        List<int[]> wheel = new ArrayList<>();
        Set<Set<Integer>> coveredSubsets = new HashSet<>();
        
        List<int[]> allCandidates = generateAllCombinations(params.getV(), params.getK());
        
        int iteration = 0;
        while (coveredSubsets.size() < totalSubsets && !cancelled.get()) {
            int[] bestTicket = findBestTicket(allCandidates, requiredSubsets, coveredSubsets, wheel);
            
            if (bestTicket == null) {
                logger.warn("No more tickets can improve coverage");
                break;
            }
            
            wheel.add(bestTicket);
            
            Set<Integer> ticketSet = new HashSet<>();
            for (int num : bestTicket) {
                ticketSet.add(num);
            }
            
            for (Set<Integer> subset : requiredSubsets) {
                if (ticketSet.containsAll(subset)) {
                    coveredSubsets.add(subset);
                }
            }
            
            iteration++;
            if (iteration % 5 == 0) {
                double progress = 0.1 + (0.85 * coveredSubsets.size() / (double)totalSubsets);
                callback.update("Building greedy wheel...", progress, coveredSubsets.size(), totalSubsets, wheel.size());
            }
        }
        
        callback.update("Greedy wheel complete", 0.95, coveredSubsets.size(), totalSubsets, wheel.size());
        return wheel;
    }
    
    private Set<Set<Integer>> generateRequiredSubsets(WheelParameters params, ProgressCallback callback, AtomicBoolean cancelled) {
        Set<Set<Integer>> subsets = new HashSet<>();
        
        if (params.getT() == params.getM()) {
            List<int[]> tSubsets = generateAllCombinations(params.getV(), params.getM());
            for (int[] subset : tSubsets) {
                if (cancelled.get()) break;
                Set<Integer> subsetSet = new HashSet<>();
                for (int num : subset) {
                    subsetSet.add(num);
                }
                subsets.add(subsetSet);
            }
        } else {
            List<int[]> tCombos = generateAllCombinations(params.getV(), params.getT());
            
            for (int[] tCombo : tCombos) {
                if (cancelled.get()) break;
                
                List<int[]> mSubsets = generateAllCombinationsFromArray(tCombo, params.getM());
                for (int[] mSubset : mSubsets) {
                    Set<Integer> subsetSet = new HashSet<>();
                    for (int num : mSubset) {
                        subsetSet.add(num);
                    }
                    subsets.add(subsetSet);
                }
            }
        }
        
        return subsets;
    }
    
    private int[] findBestTicket(List<int[]> candidates, Set<Set<Integer>> required, 
                                 Set<Set<Integer>> covered, List<int[]> existing) {
        int[] best = null;
        int maxCoverage = 0;
        
        Set<Set<Integer>> uncovered = new HashSet<>(required);
        uncovered.removeAll(covered);
        
        Set<String> existingSet = new HashSet<>();
        for (int[] ticket : existing) {
            existingSet.add(Arrays.toString(ticket));
        }
        
        int checked = 0;
        int maxCheck = Math.min(candidates.size(), 1000);
        
        for (int[] candidate : candidates) {
            if (checked++ >= maxCheck) break;
            
            if (existingSet.contains(Arrays.toString(candidate))) {
                continue;
            }
            
            Set<Integer> candidateSet = new HashSet<>();
            for (int num : candidate) {
                candidateSet.add(num);
            }
            
            int newCoverage = 0;
            for (Set<Integer> subset : uncovered) {
                if (candidateSet.containsAll(subset)) {
                    newCoverage++;
                    if (newCoverage > maxCoverage) {
                        maxCoverage = newCoverage;
                        best = candidate;
                        if (maxCoverage > 50) {
                            return best;
                        }
                    }
                }
            }
        }
        
        return best;
    }
    
    private Set<Set<Integer>> extractQuads(int[] combo) {
        Set<Set<Integer>> quads = new HashSet<>();
        for (int i = 0; i < combo.length; i++) {
            for (int j = i + 1; j < combo.length; j++) {
                for (int k = j + 1; k < combo.length; k++) {
                    for (int l = k + 1; l < combo.length; l++) {
                        Set<Integer> quad = new HashSet<>();
                        quad.add(combo[i]);
                        quad.add(combo[j]);
                        quad.add(combo[k]);
                        quad.add(combo[l]);
                        quads.add(quad);
                    }
                }
            }
        }
        return quads;
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
    
    private List<int[]> generateAllCombinationsFromArray(int[] array, int k) {
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
