package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class OptimizedWheelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedWheelBuilder.class);

    // --- PHYSICS CONSTANTS ---
    private static final int SA_ITERATIONS = 2000000;
    // FIXED: Lowered from 50.0 to 1.5 to stop "Thermal Runaway" (random walking)
    private static final double SA_INITIAL_TEMP = 1.5;
    private static final double SA_COOLING_RATE = 0.999999;
    private static final double REDUNDANCY_PENALTY = 0.0001;

    private Random random = new Random();

    public List<int[]> buildOptimalWheel(WheelParameters params, WheelProgressCallback callback, AtomicBoolean cancelled) {
        params.validate();

        logger.info("Building optimal wheel for {}", params.getNotation());
        callback.update("Analyzing coverage requirements...", 0.05, 0, 0, 0);

        Set<Set<Integer>> universe = generateUniverse(params, cancelled);
        if (cancelled.get()) return new ArrayList<>();

        int totalRequired = universe.size();
        logger.info("Universe size: {} combinations to cover", totalRequired);

        callback.update("Generating candidate tickets...", 0.1, 0, totalRequired, 0);
        List<int[]> candidates = generateCandidates(params.getV(), params.getK());
        logger.info("Generated {} candidate tickets", candidates.size());

        callback.update("Converting to BitSet...", 0.12, 0, totalRequired, 0);
        List<BitSet> universeBitSet = convertUniverseToBitSet(universe, params.getV());
        List<BitSet> candidatesBitSet = convertCandidatesToBitSet(candidates, params.getV());

        callback.update("Pre-calculating coverage matrix...", 0.15, 0, totalRequired, 0);
        BitSet[] coverageMatrix = preCalculateCoverageMatrix(candidatesBitSet, universeBitSet, params.getM());
        logger.info("Coverage matrix pre-calculated for {} candidates", candidatesBitSet.size());

        // 1. Calculate the Mathematical Floor
        int theoreticalMin = calculateTheoreticalMinimum(params, totalRequired, candidatesBitSet.size());

        // 2. Generate Baseline (Greedy) - This is our Safety Net
        callback.update("Building initial greedy wheel...", 0.2, 0, totalRequired, 0);
        Map<BitSet, Double> rarityWeights = computeRarityWeights(universeBitSet, coverageMatrix);
        List<int[]> greedyWheel = rarityWeightedGreedySelection(
                universeBitSet, candidatesBitSet, rarityWeights, params, callback, cancelled, totalRequired
        );

        if (cancelled.get()) return greedyWheel;

        int greedySize = greedyWheel.size();
        logger.info("Initial greedy wheel: {} lines", greedySize);

        // 3. Run Target-Led Optimization
        callback.update("Running target-led optimization...", 0.5, totalRequired, totalRequired, greedySize);
        List<int[]> wheel = runTargetLedOptimization(
                theoreticalMin, greedySize, greedyWheel, // Pass greedyWheel for fallback
                universeBitSet, candidatesBitSet, coverageMatrix,
                params, callback, cancelled
        );

        callback.update("Complete!", 1.0, totalRequired, totalRequired, wheel.size());
        logger.info("Final wheel size: {} lines (theoretical min: {})", wheel.size(), theoreticalMin);

        return wheel;
    }

    private int calculateTheoreticalMinimum(WheelParameters params, int universeSize, int candidatesSize) {
        int v = params.getV();
        int k = params.getK();
        int m = params.getM();
        int t = params.getT();

        long coveragePerTicket = 0;
        for (int i = m; i <= k; i++) {
            coveragePerTicket += (long) binomialCoefficient(k, i) * binomialCoefficient(v - k, t - i);
        }

        if (coveragePerTicket == 0) coveragePerTicket = 1;

        int estimate = (int) Math.ceil((double) universeSize / coveragePerTicket);
        logger.info("Coverage Ratio: 1 ticket covers {} outcomes. Floor: {}/{} = {}",
                coveragePerTicket, universeSize, coveragePerTicket, estimate);
        return estimate;
    }

    private List<int[]> runTargetLedOptimization(
            int theoreticalMin,
            int greedySize,
            List<int[]> greedyWheel, // Added fallback parameter
            List<BitSet> universe,
            List<BitSet> candidates,
            BitSet[] coverageMatrix,
            WheelParameters params,
            WheelProgressCallback callback,
            AtomicBoolean cancelled) {

        int targetSize = theoreticalMin;
        int maxTarget = greedySize;

        logger.info("DEBUG: targetSize={}, maxTarget={}, greedySize={}", targetSize, maxTarget, greedySize);
        logger.info("Starting optimization range: {} -> {}", targetSize, maxTarget);

        Map<BitSet, Integer> candidateIndexMap = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            candidateIndexMap.put(candidates.get(i), i);
        }

        while (targetSize < maxTarget && !cancelled.get()) {

            double range = Math.max(1, maxTarget - theoreticalMin);
            double currentPos = targetSize - theoreticalMin;
            double progress = 0.5 + (0.5 * (currentPos / range));
            callback.update(String.format("Optimizing %d tickets...", targetSize), progress,
                    universe.size(), universe.size(), targetSize);

            // Run High-Speed SA
            List<int[]> wheel = runFixedSizeSimulatedAnnealing(
                    targetSize, universe, candidates, coverageMatrix, params, cancelled, SA_ITERATIONS
            );

            // Verify Result
            if (verifyWheelCoverage(wheel, candidateIndexMap, coverageMatrix, universe.size())) {
                logger.info("SUCCESS: Optimal wheel found at {} lines!", targetSize);
                return wheel;
            }

            logger.info("Target {} failed. Incrementing.", targetSize);
            targetSize++;
        }

        // FIXED: Return the valid greedy wheel if optimization fails, instead of generating a broken one
        logger.warn("Optimization finished without better result. Returning valid greedy wheel ({} lines).", greedySize);
        return greedyWheel;
    }

    private List<int[]> runFixedSizeSimulatedAnnealing(
            int targetSize,
            List<BitSet> universe,
            List<BitSet> candidates,
            BitSet[] coverageMatrix,
            WheelParameters params,
            AtomicBoolean cancelled,
            int customIterations) {

        // 1. Initialize Random Wheel
        Set<Integer> usedIndices = new HashSet<>();
        List<Integer> wheelIndices = new ArrayList<>();
        while (wheelIndices.size() < targetSize) {
            int candidateIdx = random.nextInt(candidates.size());
            if (usedIndices.add(candidateIdx)) {
                wheelIndices.add(candidateIdx);
            }
        }

        // 2. Initial State Calculation
        int universeSize = universe.size();
        int[] coverageCounts = new int[universeSize];
        int currentUncovered = universeSize;
        int currentRedundancy = 0;

        for (int idx : wheelIndices) {
            BitSet coverage = coverageMatrix[idx];
            for (int drawIdx = coverage.nextSetBit(0); drawIdx >= 0; drawIdx = coverage.nextSetBit(drawIdx + 1)) {
                if (coverageCounts[drawIdx] == 0) currentUncovered--;
                if (coverageCounts[drawIdx] == 1) currentRedundancy++;
                coverageCounts[drawIdx]++;
            }
        }

        double currentEnergy = currentUncovered + (REDUNDANCY_PENALTY * currentRedundancy);
        double bestEnergy = currentEnergy;
        List<Integer> bestWheelIndices = new ArrayList<>(wheelIndices);

        double temperature = SA_INITIAL_TEMP;
        int stagnationCounter = 0;

        logger.info("Starting High-Speed SA: {} iterations, Target: {}", customIterations, targetSize);

        for (int iter = 0; iter < customIterations && !cancelled.get(); iter++) {

            int replaceIdx = random.nextInt(wheelIndices.size());
            int oldCandidateIdx = wheelIndices.get(replaceIdx);

            int newCandidateIdx;
            // 95% Chance to target holes if they exist
            if (currentUncovered > 0 && random.nextDouble() < 0.95) {
                int randomHole = findRandomHole(coverageCounts, universeSize);
                if (randomHole != -1) {
                    newCandidateIdx = findCandidateCoveringHole(randomHole, coverageMatrix, usedIndices, candidates.size());
                } else {
                    newCandidateIdx = findRandomUnusedCandidate(candidates.size(), usedIndices);
                }
            } else {
                newCandidateIdx = findRandomUnusedCandidate(candidates.size(), usedIndices);
            }

            if (newCandidateIdx == -1 || newCandidateIdx == oldCandidateIdx) continue;

            // Calculate Deltas
            int deltaUncovered = 0;
            int deltaRedundancy = 0;

            BitSet oldCoverage = coverageMatrix[oldCandidateIdx];
            for (int i = oldCoverage.nextSetBit(0); i >= 0; i = oldCoverage.nextSetBit(i + 1)) {
                if (coverageCounts[i] == 1) deltaUncovered++;
                if (coverageCounts[i] == 2) deltaRedundancy--;
            }

            BitSet newCoverage = coverageMatrix[newCandidateIdx];
            for (int i = newCoverage.nextSetBit(0); i >= 0; i = newCoverage.nextSetBit(i + 1)) {
                int countAfterRemove = coverageCounts[i] - (oldCoverage.get(i) ? 1 : 0);
                if (countAfterRemove == 0) deltaUncovered--;
                if (countAfterRemove == 1) deltaRedundancy++;
            }

            double newEnergy = (currentUncovered + deltaUncovered) + (REDUNDANCY_PENALTY * (currentRedundancy + deltaRedundancy));
            double energyDiff = newEnergy - currentEnergy;

            boolean accept = energyDiff < 0 || random.nextDouble() < Math.exp(-energyDiff / temperature);

            if (accept) {
                wheelIndices.set(replaceIdx, newCandidateIdx);
                usedIndices.remove(oldCandidateIdx);
                usedIndices.add(newCandidateIdx);

                for (int i = oldCoverage.nextSetBit(0); i >= 0; i = oldCoverage.nextSetBit(i + 1)) coverageCounts[i]--;
                for (int i = newCoverage.nextSetBit(0); i >= 0; i = newCoverage.nextSetBit(i + 1)) coverageCounts[i]++;

                currentUncovered += deltaUncovered;
                currentRedundancy += deltaRedundancy;
                currentEnergy = newEnergy;
                stagnationCounter = 0;

                if (currentEnergy < bestEnergy) {
                    bestEnergy = currentEnergy;
                    bestWheelIndices = new ArrayList<>(wheelIndices);
                    if (currentUncovered == 0) {
                        logger.info("SA SUCCESS: 100% Coverage at iter {}", iter);
                        break;
                    }
                }
            } else {
                stagnationCounter++;
            }

            temperature *= SA_COOLING_RATE;

            // Reheat if stuck
            if (stagnationCounter > 250000) {
                temperature += 0.5; // Mild reheat
                stagnationCounter = 0;
            }

            if (iter % 200000 == 0 && iter > 0) {
                logger.info("Iter {}/{}: uncovered={}, temp={:.5f}", iter, customIterations, currentUncovered, temperature);
            }
        }

        return bestWheelIndices.stream()
                .map(idx -> bitSetToArray(candidates.get(idx)))
                .collect(Collectors.toList());
    }

    private int findRandomHole(int[] coverageCounts, int universeSize) {
        int start = random.nextInt(universeSize);
        for (int i = 0; i < universeSize; i++) {
            int idx = (start + i) % universeSize;
            if (coverageCounts[idx] == 0) return idx;
        }
        return -1;
    }

    private int findCandidateCoveringHole(int holeIdx, BitSet[] coverageMatrix, Set<Integer> usedIndices, int totalCandidates) {
        int start = random.nextInt(totalCandidates);
        for (int i = 0; i < totalCandidates; i++) {
            int idx = (start + i) % totalCandidates;
            if (!usedIndices.contains(idx) && coverageMatrix[idx].get(holeIdx)) {
                return idx;
            }
        }
        return -1;
    }

    private int findRandomUnusedCandidate(int candidatesSize, Set<Integer> usedIndices) {
        for(int i=0; i<50; i++) {
            int idx = random.nextInt(candidatesSize);
            if(!usedIndices.contains(idx)) return idx;
        }
        return -1;
    }

    private boolean verifyWheelCoverage(List<int[]> wheel, Map<BitSet, Integer> indexMap, BitSet[] coverageMatrix, int universeSize) {
        int[] coverageCounts = new int[universeSize];
        for (int[] ticket : wheel) {
            BitSet bs = arrayToBitSet(ticket);
            Integer idx = indexMap.get(bs);
            if (idx != null) {
                BitSet coverage = coverageMatrix[idx];
                for (int drawIdx = coverage.nextSetBit(0); drawIdx >= 0; drawIdx = coverage.nextSetBit(drawIdx + 1)) {
                    coverageCounts[drawIdx]++;
                }
            }
        }
        int covered = 0;
        for(int c : coverageCounts) {
            if(c > 0) covered++;
        }
        return covered == universeSize;
    }

    private double binomialCoefficient(int n, int k) {
        if (k > n || k < 0) return 0;
        if (k == 0 || k == n) return 1;
        double result = 1;
        for (int i = 0; i < Math.min(k, n - k); i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    private int[] bitSetToArray(BitSet bs) {
        return bs.stream().toArray();
    }

    private BitSet arrayToBitSet(int[] ticket) {
        BitSet bs = new BitSet();
        for (int num : ticket) bs.set(num);
        return bs;
    }

    // --- GREEDY SETUP METHODS ---

    private Set<Set<Integer>> generateUniverse(WheelParameters params, AtomicBoolean cancelled) {
        Set<Set<Integer>> universe = new HashSet<>();
        List<int[]> tCombos = generateAllCombinations(params.getV(), params.getT());
        for (int[] tCombo : tCombos) {
            if (cancelled.get()) break;
            universe.add(arrayToSet(tCombo));
        }
        return universe;
    }

    private List<int[]> generateCandidates(int v, int k) {
        return generateAllCombinations(v, k);
    }

    private List<BitSet> convertUniverseToBitSet(Set<Set<Integer>> universe, int v) {
        return universe.stream().map(draw -> {
            BitSet bs = new BitSet(v + 1);
            for (int num : draw) bs.set(num);
            return bs;
        }).collect(Collectors.toList());
    }

    private List<BitSet> convertCandidatesToBitSet(List<int[]> candidates, int v) {
        return candidates.stream().map(ticket -> {
            BitSet bs = new BitSet(v + 1);
            for (int num : ticket) bs.set(num);
            return bs;
        }).collect(Collectors.toList());
    }

    private BitSet[] preCalculateCoverageMatrix(List<BitSet> candidates, List<BitSet> universe, int m) {
        BitSet[] matrix = new BitSet[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            matrix[i] = new BitSet(universe.size());
            BitSet candidate = candidates.get(i);
            for (int j = 0; j < universe.size(); j++) {
                BitSet intersection = (BitSet) candidate.clone();
                intersection.and(universe.get(j));
                if (intersection.cardinality() >= m) {
                    matrix[i].set(j);
                }
            }
        }
        return matrix;
    }

    private Map<BitSet, Double> computeRarityWeights(List<BitSet> universe, BitSet[] coverageMatrix) {
        Map<BitSet, Double> weights = new HashMap<>();
        int[] drawCoverageCounts = new int[universe.size()];
        for (BitSet coverage : coverageMatrix) {
            for (int i = coverage.nextSetBit(0); i >= 0; i = coverage.nextSetBit(i + 1)) {
                drawCoverageCounts[i]++;
            }
        }
        for (int i = 0; i < universe.size(); i++) {
            weights.put(universe.get(i), drawCoverageCounts[i] > 0 ? 1.0 / drawCoverageCounts[i] : 1000.0);
        }
        return weights;
    }

    private List<int[]> rarityWeightedGreedySelection(List<BitSet> universe, List<BitSet> candidates, Map<BitSet, Double> rarityWeights, WheelParameters params, WheelProgressCallback callback, AtomicBoolean cancelled, int totalRequired) {
        List<int[]> wheel = new ArrayList<>();
        Set<BitSet> uncovered = new HashSet<>(universe);
        Set<Integer> usedIndices = new HashSet<>();

        while (!uncovered.isEmpty() && !cancelled.get()) {
            int bestIdx = -1;
            double maxScore = -1;

            for (int i = 0; i < candidates.size(); i++) {
                if (usedIndices.contains(i)) continue;
                double score = 0;
                BitSet candidate = candidates.get(i);
                for (BitSet draw : uncovered) {
                    BitSet inter = (BitSet) candidate.clone();
                    inter.and(draw);
                    if (inter.cardinality() >= params.getM()) {
                        score += rarityWeights.getOrDefault(draw, 1.0);
                    }
                }
                if (score > maxScore) {
                    maxScore = score;
                    bestIdx = i;
                }
            }

            if (bestIdx == -1) break;

            BitSet bestTicket = candidates.get(bestIdx);
            wheel.add(bitSetToArray(bestTicket));
            usedIndices.add(bestIdx);

            Set<BitSet> newlyCovered = new HashSet<>();
            for (BitSet draw : uncovered) {
                BitSet inter = (BitSet) bestTicket.clone();
                inter.and(draw);
                if (inter.cardinality() >= params.getM()) newlyCovered.add(draw);
            }
            uncovered.removeAll(newlyCovered);
        }
        return wheel;
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
        for (int num : array) set.add(num);
        return set;
    }
}