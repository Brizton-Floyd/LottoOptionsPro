package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.*;
import com.example.lottooptionspro.util.CombinatoricsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AbbreviatedWheelService {
    
    private static final Logger logger = LoggerFactory.getLogger(AbbreviatedWheelService.class);
    private final WheelTableService wheelTableService;
    private final PatternBasedWheelGenerator patternWheelGenerator;
    
    public AbbreviatedWheelService(WheelTableService wheelTableService, 
                                  PatternBasedWheelGenerator patternWheelGenerator) {
        this.wheelTableService = wheelTableService;
        this.patternWheelGenerator = patternWheelGenerator;
    }

    public WheelGenerationResponse generateWheel(WheelConfiguration config) {
        config.validate();
        
        WheelParameters params = new WheelParameters(
            config.getPoolSize(), 
            config.getPickSize(), 
            config.getGuaranteeLevel()
        );
        
        List<int[]> templateWheel = patternWheelGenerator.generateFromTemplate(params);
        
        List<int[]> combinations;
        if (templateWheel != null) {
            logger.info("Using pre-computed wheel: {}-{}-{}-{} ({} lines)", 
                       params.getV(), params.getK(), params.getT(), params.getM(), templateWheel.size());
            combinations = applyPatterns(config.getNumbers(), templateWheel.toArray(new int[0][]));
        } else {
            logger.warn("No pre-computed wheel found for {}-{}-{}-{}, using algorithm", 
                       params.getV(), params.getK(), params.getT(), params.getM());
            switch (config.getPickSize()) {
                case 6:
                    combinations = generatePick6Wheel(config);
                    break;
                case 5:
                    combinations = generatePick5Wheel(config);
                    break;
                case 4:
                    combinations = generatePick4Wheel(config);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported pick size: " + config.getPickSize());
            }
        }
        
        WheelStatistics statistics = WheelStatistics.calculate(
            combinations.size(),
            config.getPoolSize(),
            config.getPickSize(),
            config.getGuaranteeLevel()
        );
        
        return new WheelGenerationResponse(
            combinations,
            combinations.size(),
            config.getGuaranteeLevel(),
            statistics
        );
    }

    private List<int[]> generatePick6Wheel(WheelConfiguration config) {
        switch (config.getGuaranteeLevel()) {
            case FIVE_IF_SIX:
                return generateFiveIfSixWheel(config.getNumbers());
            case FOUR_IF_SIX:
                return generateFourIfSixWheel(config.getNumbers());
            case FOUR_IF_FIVE:
                return generateFourIfFiveWheel(config.getNumbers());
            case FOUR_IF_FOUR:
                return generateFourIfFourWheel(config.getNumbers());
            default:
                throw new IllegalArgumentException("Invalid guarantee level for Pick-6");
        }
    }

    private List<int[]> generatePick5Wheel(WheelConfiguration config) {
        switch (config.getGuaranteeLevel()) {
            case FOUR_IF_FIVE_P5:
                return generateFourIfFivePick5Wheel(config.getNumbers());
            case FOUR_IF_FOUR_P5:
                return generateFourIfFourPick5Wheel(config.getNumbers());
            case THREE_IF_FIVE:
                return generateThreeIfFiveWheel(config.getNumbers());
            case THREE_IF_FOUR_P5:
                return generateThreeIfFourPick5Wheel(config.getNumbers());
            default:
                throw new IllegalArgumentException("Invalid guarantee level for Pick-5");
        }
    }

    private List<int[]> generatePick4Wheel(WheelConfiguration config) {
        switch (config.getGuaranteeLevel()) {
            case THREE_IF_FOUR:
                return generateThreeIfFourPick4Wheel(config.getNumbers());
            case THREE_IF_THREE:
                return generateThreeIfThreeWheel(config.getNumbers());
            case TWO_IF_FOUR:
                return generateTwoIfFourWheel(config.getNumbers());
            case TWO_IF_THREE:
                return generateTwoIfThreeWheel(config.getNumbers());
            default:
                throw new IllegalArgumentException("Invalid guarantee level for Pick-4");
        }
    }

    private List<int[]> generateFiveIfSixWheel(List<Integer> numbers) {
        int n = numbers.size();
        
        if (n <= 7) {
            return CombinatoricsUtil.generateAllCombinations(numbers, 6);
        }
        
        return generateGreedyCoveringWheel(numbers, 6, 5, 6);
    }

    private List<int[]> generateFourIfSixWheel(List<Integer> numbers) {
        int n = numbers.size();
        
        if (n <= 9) {
            return getOptimizedFourIfSixWheel(numbers);
        }
        
        return generateGreedyCoveringWheel(numbers, 6, 4, 6);
    }

    private List<int[]> generateFourIfFiveWheel(List<Integer> numbers) {
        return generateGreedyCoveringWheel(numbers, 6, 4, 5);
    }

    private List<int[]> generateFourIfFourWheel(List<Integer> numbers) {
        return generateGreedyCoveringWheel(numbers, 6, 4, 4);
    }

    private List<int[]> generateFourIfFivePick5Wheel(List<Integer> numbers) {
        int n = numbers.size();
        
        if (n <= 8) {
            return CombinatoricsUtil.generateAllCombinations(numbers, 5);
        }
        
        return generateGreedyCoveringWheel(numbers, 5, 4, 5);
    }

    private List<int[]> generateFourIfFourPick5Wheel(List<Integer> numbers) {
        return generateGreedyCoveringWheel(numbers, 5, 4, 4);
    }

    private List<int[]> generateThreeIfFiveWheel(List<Integer> numbers) {
        int n = numbers.size();
        
        if (n <= 10) {
            return getOptimizedThreeIfFiveWheel(numbers);
        }
        
        return generateGreedyCoveringWheel(numbers, 5, 3, 5);
    }

    private List<int[]> generateThreeIfFourPick5Wheel(List<Integer> numbers) {
        return generateGreedyCoveringWheel(numbers, 5, 3, 4);
    }

    private List<int[]> generateThreeIfFourPick4Wheel(List<Integer> numbers) {
        int n = numbers.size();
        
        if (n <= 7) {
            return CombinatoricsUtil.generateAllCombinations(numbers, 4);
        }
        
        return generateGreedyCoveringWheel(numbers, 4, 3, 4);
    }

    private List<int[]> generateThreeIfThreeWheel(List<Integer> numbers) {
        return generateGreedyCoveringWheel(numbers, 4, 3, 3);
    }

    private List<int[]> generateTwoIfFourWheel(List<Integer> numbers) {
        return generateGreedyCoveringWheel(numbers, 4, 2, 4);
    }

    private List<int[]> generateTwoIfThreeWheel(List<Integer> numbers) {
        return generateGreedyCoveringWheel(numbers, 4, 2, 3);
    }

    private List<int[]> getOptimizedFourIfSixWheel(List<Integer> numbers) {
        int n = numbers.size();
        
        if (n == 7) {
            return getOptimized7NumberFourIfSix(numbers);
        } else if (n == 8) {
            return getOptimized8NumberFourIfSix(numbers);
        } else if (n == 9) {
            return getOptimized9NumberFourIfSix(numbers);
        }
        
        return CombinatoricsUtil.generateAllCombinations(numbers, 6);
    }

    private List<int[]> getOptimized7NumberFourIfSix(List<Integer> numbers) {
        int[][] patterns = {
            {0, 1, 2, 3, 4, 5},
            {0, 1, 2, 3, 4, 6},
            {0, 1, 2, 3, 5, 6},
            {0, 1, 2, 4, 5, 6},
            {0, 1, 3, 4, 5, 6},
            {0, 2, 3, 4, 5, 6},
            {1, 2, 3, 4, 5, 6}
        };
        
        return applyPatterns(numbers, patterns);
    }

    private List<int[]> getOptimized8NumberFourIfSix(List<Integer> numbers) {
        int[][] patterns = {
            {0, 1, 2, 3, 4, 5},
            {0, 1, 2, 3, 6, 7},
            {0, 1, 4, 5, 6, 7},
            {0, 2, 3, 4, 6, 7},
            {0, 2, 3, 5, 6, 7},
            {0, 3, 4, 5, 6, 7},
            {1, 2, 3, 4, 5, 6},
            {1, 2, 4, 5, 6, 7},
            {1, 3, 4, 5, 6, 7},
            {2, 3, 4, 5, 6, 7}
        };
        
        return applyPatterns(numbers, patterns);
    }

    private List<int[]> getOptimized9NumberFourIfSix(List<Integer> numbers) {
        int[][] patterns = {
            {0, 1, 2, 3, 4, 5},
            {0, 1, 2, 6, 7, 8},
            {0, 1, 3, 4, 6, 7},
            {0, 1, 3, 5, 7, 8},
            {0, 2, 3, 4, 7, 8},
            {0, 2, 4, 5, 6, 8},
            {0, 3, 5, 6, 7, 8},
            {1, 2, 3, 5, 6, 7},
            {1, 2, 4, 5, 7, 8},
            {1, 3, 4, 6, 7, 8},
            {1, 4, 5, 6, 7, 8},
            {2, 3, 4, 5, 6, 8},
            {2, 3, 5, 6, 7, 8}
        };
        
        return applyPatterns(numbers, patterns);
    }

    private List<int[]> getOptimizedThreeIfFiveWheel(List<Integer> numbers) {
        int n = numbers.size();
        
        if (n == 7) {
            return getOptimized7NumberThreeIfFive(numbers);
        } else if (n == 8) {
            return getOptimized8NumberThreeIfFive(numbers);
        } else if (n == 9) {
            return getOptimized9NumberThreeIfFive(numbers);
        } else if (n == 10) {
            return getOptimized10NumberThreeIfFive(numbers);
        }
        
        return CombinatoricsUtil.generateAllCombinations(numbers, 5);
    }

    private List<int[]> getOptimized7NumberThreeIfFive(List<Integer> numbers) {
        int[][] patterns = {
            {0, 1, 2, 3, 4},
            {0, 1, 2, 5, 6},
            {0, 3, 4, 5, 6},
            {1, 2, 3, 5, 6},
            {1, 3, 4, 5, 6},
            {2, 3, 4, 5, 6}
        };
        
        return applyPatterns(numbers, patterns);
    }

    private List<int[]> getOptimized8NumberThreeIfFive(List<Integer> numbers) {
        int[][] patterns = {
            {0, 1, 2, 3, 4},
            {0, 1, 2, 5, 6},
            {0, 1, 3, 6, 7},
            {0, 2, 4, 5, 7},
            {0, 3, 4, 6, 7},
            {1, 2, 3, 5, 7},
            {1, 3, 4, 5, 6},
            {2, 4, 5, 6, 7},
            {3, 4, 5, 6, 7}
        };
        
        return applyPatterns(numbers, patterns);
    }

    private List<int[]> getOptimized9NumberThreeIfFive(List<Integer> numbers) {
        int[][] patterns = {
            {0, 1, 2, 3, 4},
            {0, 1, 2, 5, 6},
            {0, 1, 3, 7, 8},
            {0, 2, 4, 5, 7},
            {0, 3, 4, 6, 8},
            {0, 5, 6, 7, 8},
            {1, 2, 3, 5, 8},
            {1, 3, 4, 5, 7},
            {1, 4, 6, 7, 8},
            {2, 3, 6, 7, 8},
            {2, 4, 5, 6, 8},
            {3, 4, 5, 6, 7}
        };
        
        return applyPatterns(numbers, patterns);
    }

    private List<int[]> getOptimized10NumberThreeIfFive(List<Integer> numbers) {
        int[][] patterns = {
            {0, 1, 2, 3, 4},
            {0, 1, 2, 5, 6},
            {0, 1, 3, 7, 8},
            {0, 2, 4, 5, 9},
            {0, 3, 4, 6, 9},
            {0, 5, 7, 8, 9},
            {1, 2, 3, 5, 9},
            {1, 3, 4, 5, 7},
            {1, 4, 6, 8, 9},
            {1, 6, 7, 8, 9},
            {2, 3, 6, 7, 9},
            {2, 4, 6, 7, 8},
            {3, 4, 5, 6, 8},
            {3, 5, 6, 7, 8},
            {4, 5, 6, 7, 9}
        };
        
        return applyPatterns(numbers, patterns);
    }

    private List<int[]> applyPatterns(List<Integer> numbers, int[][] patterns) {
        List<int[]> result = new ArrayList<>();
        
        for (int[] pattern : patterns) {
            int[] combination = new int[pattern.length];
            for (int i = 0; i < pattern.length; i++) {
                combination[i] = numbers.get(pattern[i]);
            }
            Arrays.sort(combination);
            result.add(combination);
        }
        
        return result;
    }

    private List<int[]> generateGreedyCoveringWheel(List<Integer> numbers, int pickSize, int guaranteedMatches, int requiredHits) {
        int poolSize = numbers.size();
        
        if (poolSize <= pickSize + 3) {
            return CombinatoricsUtil.generateAllCombinations(numbers, pickSize);
        }
        
        return generateGuaranteedCoveringWheel(numbers, pickSize, guaranteedMatches, requiredHits);
    }
    
    private List<int[]> generateGuaranteedCoveringWheel(List<Integer> numbers, int pickSize, int guaranteedMatches, int requiredHits) {
        List<int[]> wheel = new ArrayList<>();
        
        Set<Set<Integer>> requiredSubsets = generateRequiredSubsets(numbers, requiredHits, guaranteedMatches);
        Set<Set<Integer>> coveredSubsets = new HashSet<>();
        
        while (coveredSubsets.size() < requiredSubsets.size()) {
            int[] bestCombo = findBestCombination(numbers, pickSize, guaranteedMatches, requiredSubsets, coveredSubsets, wheel);
            wheel.add(bestCombo);
            
            Set<Integer> comboSet = new HashSet<>();
            for (int num : bestCombo) {
                comboSet.add(num);
            }
            
            for (Set<Integer> subset : requiredSubsets) {
                if (comboSet.containsAll(subset)) {
                    coveredSubsets.add(subset);
                }
            }
        }
        
        return new ArrayList<>(wheel);
    }
    
    private Set<Set<Integer>> generateRequiredSubsets(List<Integer> numbers, int requiredHits, int guaranteedMatches) {
        Set<Set<Integer>> subsets = new HashSet<>();
        
        List<int[]> hitCombinations = CombinatoricsUtil.generateAllCombinations(numbers, requiredHits);
        
        for (int[] hitCombo : hitCombinations) {
            List<Integer> hitList = new ArrayList<>();
            for (int num : hitCombo) {
                hitList.add(num);
            }
            
            List<int[]> matchSubsets = CombinatoricsUtil.generateAllCombinations(hitList, guaranteedMatches);
            for (int[] matchSubset : matchSubsets) {
                Set<Integer> subset = new HashSet<>();
                for (int num : matchSubset) {
                    subset.add(num);
                }
                subsets.add(subset);
            }
        }
        
        return subsets;
    }
    
    private int[] findBestCombination(List<Integer> numbers, int pickSize, int guaranteedMatches, 
                                     Set<Set<Integer>> requiredSubsets, Set<Set<Integer>> coveredSubsets,
                                     List<int[]> existingWheel) {
        int[] bestCombo = null;
        int maxNewCoverage = 0;
        
        List<int[]> candidates = CombinatoricsUtil.generateAllCombinations(numbers, pickSize);
        
        for (int[] candidate : candidates) {
            boolean alreadyExists = false;
            for (int[] existing : existingWheel) {
                if (Arrays.equals(existing, candidate)) {
                    alreadyExists = true;
                    break;
                }
            }
            
            if (alreadyExists) continue;
            
            Set<Integer> candidateSet = new HashSet<>();
            for (int num : candidate) {
                candidateSet.add(num);
            }
            
            int newCoverage = 0;
            for (Set<Integer> subset : requiredSubsets) {
                if (!coveredSubsets.contains(subset) && candidateSet.containsAll(subset)) {
                    newCoverage++;
                }
            }
            
            if (newCoverage > maxNewCoverage) {
                maxNewCoverage = newCoverage;
                bestCombo = candidate;
            }
        }
        
        if (bestCombo == null) {
            bestCombo = candidates.get(0);
        }
        
        return bestCombo;
    }

    public long estimateLineCount(int poolSize, int pickSize, GuaranteeLevel guarantee) {
        if (poolSize < 4 || poolSize > 27) {
            throw new IllegalArgumentException("Pool size must be between 4 and 27");
        }
        
        List<Integer> dummyNumbers = new ArrayList<>();
        for (int i = 1; i <= poolSize; i++) {
            dummyNumbers.add(i);
        }
        
        WheelConfiguration config = new WheelConfiguration(poolSize, pickSize, dummyNumbers, guarantee);
        WheelGenerationResponse response = generateWheel(config);
        
        return response.getTotalLines();
    }
}
