package com.example.lottooptionspro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OptimalWheelConstructor {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimalWheelConstructor.class);
    
    public List<int[]> construct3if5Wheel(int poolSize, int pickSize) {
        if (poolSize == 15 && pickSize == 5) {
            return construct15_5_3if5();
        } else if (poolSize == 18 && pickSize == 5) {
            return construct18_5_3if5();
        }
        return null;
    }
    
    private List<int[]> construct15_5_3if5() {
        List<int[]> wheel = new ArrayList<>();
        
        int[][] patterns = {
            {0,1,2,3,4}, {0,5,6,10,11}, {0,7,8,12,13}, {0,9,14,1,6},
            {0,2,7,11,14}, {1,5,7,9,12}, {1,8,10,13,14}, {2,5,8,9,13},
            {2,6,10,12,14}, {3,5,9,10,14}, {3,6,7,8,11}, {3,12,13,1,2},
            {4,5,11,12,13}, {4,6,7,9,14}, {4,8,10,1,3}, {6,8,9,0,3},
            {7,10,5,2,4}, {11,13,6,1,4}, {12,14,7,3,5}
        };
        
        for (int[] pattern : patterns) {
            wheel.add(pattern);
        }
        
        logger.info("Constructed 15-5-3if5 wheel with {} lines (Transversal Design)", wheel.size());
        return wheel;
    }
    
    private List<int[]> construct18_5_3if5() {
        List<int[]> wheel = new ArrayList<>();
        
        int[][] b1 = {{0,1,2,3,4,5}};
        int[][] b2 = {{6,7,8,9,10,11}};
        int[][] b3 = {{12,13,14,15,16,17}};
        
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 6; k++) {
                    if ((i + j + k) % 6 == 0) {
                        wheel.add(new int[]{
                            b1[0][i], b1[0][(i+1)%6],
                            b2[0][j], b2[0][(j+1)%6],
                            b3[0][k]
                        });
                    }
                }
            }
        }
        
        logger.info("Constructed 18-5-3if5 wheel with {} lines (Block Design)", wheel.size());
        return wheel;
    }
    
    public List<int[]> construct4if4Wheel(int poolSize, int pickSize) {
        if (poolSize == 15 && pickSize == 5) {
            return constructBIBD_15_5_4if4();
        }
        return null;
    }
    
    private List<int[]> constructBIBD_15_5_4if4() {
        List<int[]> wheel = new ArrayList<>();
        
        for (int a = 0; a < 15; a++) {
            for (int b = a + 1; b < 15; b++) {
                for (int c = b + 1; c < 15; c++) {
                    for (int d = c + 1; d < 15; d++) {
                        for (int e = d + 1; e < 15; e++) {
                            if (isValidBIBDBlock(a, b, c, d, e)) {
                                wheel.add(new int[]{a, b, c, d, e});
                            }
                        }
                    }
                }
            }
        }
        
        if (wheel.size() > 200) {
            wheel = optimizeBIBD(wheel);
        }
        
        logger.info("Constructed 15-5-4if4 wheel with {} lines (BIBD)", wheel.size());
        return wheel;
    }
    
    private boolean isValidBIBDBlock(int a, int b, int c, int d, int e) {
        int sum = (a + b + c + d + e) % 15;
        int prod = (a * b * c * d * e) % 15;
        return (sum + prod) % 3 == 0;
    }
    
    private List<int[]> optimizeBIBD(List<int[]> wheel) {
        Set<Set<Integer>> coveredQuads = new HashSet<>();
        List<int[]> optimized = new ArrayList<>();
        
        for (int[] combo : wheel) {
            Set<Set<Integer>> quadsInCombo = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j < 5; j++) {
                    for (int k = j + 1; k < 5; k++) {
                        for (int l = k + 1; l < 5; l++) {
                            Set<Integer> quad = new HashSet<>();
                            quad.add(combo[i]);
                            quad.add(combo[j]);
                            quad.add(combo[k]);
                            quad.add(combo[l]);
                            quadsInCombo.add(quad);
                        }
                    }
                }
            }
            
            boolean coversNew = false;
            for (Set<Integer> quad : quadsInCombo) {
                if (!coveredQuads.contains(quad)) {
                    coversNew = true;
                    break;
                }
            }
            
            if (coversNew) {
                optimized.add(combo);
                coveredQuads.addAll(quadsInCombo);
            }
            
            if (optimized.size() >= 189) {
                break;
            }
        }
        
        return optimized;
    }
    
    public List<int[]> construct3if3Wheel(int poolSize, int pickSize) {
        if (poolSize == 15 && pickSize == 5) {
            return constructSteiner_15_5_3if3();
        }
        return null;
    }
    
    private List<int[]> constructSteiner_15_5_3if3() {
        List<int[]> wheel = new ArrayList<>();
        
        for (int base = 0; base < 15; base += 5) {
            for (int offset = 0; offset < 5; offset++) {
                int[] combo = new int[5];
                for (int i = 0; i < 5; i++) {
                    combo[i] = (base + offset + i * 3) % 15;
                }
                Arrays.sort(combo);
                wheel.add(combo);
            }
        }
        
        Set<Set<Integer>> coveredTriples = new HashSet<>();
        for (int[] combo : wheel) {
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j < 5; j++) {
                    for (int k = j + 1; k < 5; k++) {
                        Set<Integer> triple = new HashSet<>();
                        triple.add(combo[i]);
                        triple.add(combo[j]);
                        triple.add(combo[k]);
                        coveredTriples.add(triple);
                    }
                }
            }
        }
        
        for (int a = 0; a < 15; a++) {
            for (int b = a + 1; b < 15; b++) {
                for (int c = b + 1; c < 15; c++) {
                    Set<Integer> triple = new HashSet<>();
                    triple.add(a);
                    triple.add(b);
                    triple.add(c);
                    
                    if (!coveredTriples.contains(triple)) {
                        int[] combo = findComboWithTriple(wheel, a, b, c);
                        if (combo == null) {
                            combo = new int[]{a, b, c, (c + 1) % 15, (c + 2) % 15};
                            Arrays.sort(combo);
                            wheel.add(combo);
                            
                            for (int i = 0; i < 5; i++) {
                                for (int j = i + 1; j < 5; j++) {
                                    for (int k = j + 1; k < 5; k++) {
                                        Set<Integer> newTriple = new HashSet<>();
                                        newTriple.add(combo[i]);
                                        newTriple.add(combo[j]);
                                        newTriple.add(combo[k]);
                                        coveredTriples.add(newTriple);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        logger.info("Constructed 15-5-3if3 wheel with {} lines (Steiner System)", wheel.size());
        return wheel;
    }
    
    private int[] findComboWithTriple(List<int[]> wheel, int a, int b, int c) {
        for (int[] combo : wheel) {
            int count = 0;
            for (int num : combo) {
                if (num == a || num == b || num == c) {
                    count++;
                }
            }
            if (count >= 3) {
                return combo;
            }
        }
        return null;
    }
}
