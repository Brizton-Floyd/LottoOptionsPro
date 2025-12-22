package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class OptimizedWheelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedWheelBuilder.class);

    // PHYSICS CONSTANTS
    private static final int SA_ITERATIONS = 2000000;
    private static final double SA_INITIAL_TEMP = 1.5;
    private static final double SA_COOLING_RATE = 0.999999;
    private static final double REDUNDANCY_PENALTY = 0.0001;

    // THREADING
    private final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    private Random random = new Random();

    public List<int[]> buildOptimalWheel(WheelParameters params, WheelProgressCallback callback, AtomicBoolean cancelled) {
        params.validate();

        logger.info("Building optimal wheel for {}", params.getNotation());
        logger.info("Parallel Processing: Using {} threads.", THREAD_COUNT);

        callback.update("Analyzing coverage requirements...", 0.05, 0, 0, 0);

        Set<Set<Integer>> universe = generateUniverse(params, cancelled);
        if (cancelled.get()) return new ArrayList<>();

        int totalRequired = universe.size();

        callback.update("Generating candidate tickets...", 0.1, 0, totalRequired, 0);
        List<int[]> candidates = generateCandidates(params.getV(), params.getK());

        callback.update("Converting to BitSet...", 0.12, 0, totalRequired, 0);
        List<BitSet> universeBitSet = convertUniverseToBitSet(universe, params.getV());
        List<BitSet> candidatesBitSet = convertCandidatesToBitSet(candidates, params.getV());

        // OPTIMIZATION: Parallel Pre-calc
        callback.update("Pre-calculating coverage matrix (Parallel)...", 0.15, 0, totalRequired, 0);
        long startM = System.currentTimeMillis();
        BitSet[] coverageMatrix = preCalculateCoverageMatrix(candidatesBitSet, universeBitSet, params.getM());
        logger.info("Coverage matrix calc took {}ms", System.currentTimeMillis() - startM);

        // 1. Calculate Floor
        int theoreticalMin = calculateTheoreticalMinimum(params, totalRequired, candidatesBitSet.size());

        // 2. Generate Baseline (Matrix-Based Greedy)
        callback.update("Building initial greedy wheel...", 0.2, 0, totalRequired, 0);
        long startG = System.currentTimeMillis();
        List<int[]> greedyWheel = matrixBasedGreedySelection(
                candidatesBitSet, coverageMatrix, totalRequired, callback, cancelled
        );
        logger.info("Greedy generation took {}ms", System.currentTimeMillis() - startG);

        if (cancelled.get()) return greedyWheel;

        int greedySize = greedyWheel.size();
        logger.info("Initial greedy wheel: {} lines", greedySize);

        // 3. Run PARALLEL Target-Led Optimization
        callback.update("Running multi-threaded optimization...", 0.5, totalRequired, totalRequired, greedySize);

        List<int[]> wheel = runParallelTargetLedOptimization(
                theoreticalMin, greedySize, greedyWheel,
                universeBitSet, candidatesBitSet, coverageMatrix,
                params, callback, cancelled
        );

        callback.update("Complete!", 1.0, totalRequired, totalRequired, wheel.size());
        logger.info("Final wheel size: {} lines (theoretical min: {})", wheel.size(), theoreticalMin);

        return wheel;
    }

    private List<int[]> runParallelTargetLedOptimization(
            int theoreticalMin,
            int greedySize,
            List<int[]> greedyWheel,
            List<BitSet> universe,
            List<BitSet> candidates,
            BitSet[] coverageMatrix,
            WheelParameters params,
            WheelProgressCallback callback,
            AtomicBoolean cancelled) {

        // OPTIMIZATION: Bump heuristic to 0.65 to skip more impossible ranges
        int heuristicStart = (int) (greedySize * 0.65);
        int targetSize = Math.max(theoreticalMin, heuristicStart);
        int maxTarget = greedySize;

        logger.info("Optimization Range: {} -> {} (Skipped theoretical floor {})", targetSize, maxTarget, theoreticalMin);

        Map<BitSet, Integer> candidateIndexMap = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            candidateIndexMap.put(candidates.get(i), i);
        }

        while (targetSize < maxTarget && !cancelled.get()) {

            double progress = 0.5 + (0.5 * ((double)(targetSize - theoreticalMin) / Math.max(1, maxTarget - theoreticalMin)));
            callback.update(String.format("Optimizing %d tickets (%d threads)...", targetSize, THREAD_COUNT), progress,
                    universe.size(), universe.size(), targetSize);

            ExecutorCompletionService<List<int[]>> completionService = new ExecutorCompletionService<>(executor);
            List<Future<List<int[]>>> futures = new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int currentTarget = targetSize;
                futures.add(completionService.submit(() ->
                        runFixedSizeSimulatedAnnealing(
                                currentTarget, universe, candidates, coverageMatrix, params, cancelled, SA_ITERATIONS
                        )
                ));
            }

            int finishers = 0;
            boolean success = false;
            List<int[]> winningWheel = null;

            try {
                while (finishers < THREAD_COUNT) {
                    Future<List<int[]>> resultFuture = completionService.take();
                    finishers++;

                    List<int[]> result = resultFuture.get();
                    if (result != null && !result.isEmpty() && verifyWheelCoverage(result, candidateIndexMap, coverageMatrix, universe.size())) {
                        logger.info("SUCCESS: Thread found optimal wheel at {} lines!", targetSize);
                        winningWheel = result;
                        success = true;
                        break;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Thread interruption", e);
            }

            for (Future<?> f : futures) f.cancel(true);

            if (success) return winningWheel;

            logger.info("Target {} failed on all threads. Incrementing.", targetSize);
            targetSize++;
        }

        logger.warn("Optimization finished without hitting 100%. Returning greedy attempt.");
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

        ThreadLocalRandom threadRandom = ThreadLocalRandom.current();

        Set<Integer> usedIndices = new HashSet<>();
        List<Integer> wheelIndices = new ArrayList<>();
        while (wheelIndices.size() < targetSize) {
            int candidateIdx = threadRandom.nextInt(candidates.size());
            if (usedIndices.add(candidateIdx)) {
                wheelIndices.add(candidateIdx);
            }
        }

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

        for (int iter = 0; iter < customIterations && !cancelled.get(); iter++) {
            if (Thread.currentThread().isInterrupted()) return new ArrayList<>();

            // --- AGGRESSIVE FAIL-FAST CHECKPOINTS ---
            // Abort early if the wheel isn't shaping up fast enough

            // Check 1: At 2.5% progress (50k iters), must cover >50%
            if (iter == 50000) {
                double coverageRatio = (double)(universeSize - currentUncovered) / universeSize;
                if (coverageRatio < 0.50) return new ArrayList<>(); // Abort
            }
            // Check 2: At 10% progress (200k iters), must cover >85%
            if (iter == 200000) {
                double coverageRatio = (double)(universeSize - currentUncovered) / universeSize;
                if (coverageRatio < 0.85) return new ArrayList<>(); // Abort
            }
            // Check 3: At 25% progress (500k iters), must cover >95%
            if (iter == 500000) {
                double coverageRatio = (double)(universeSize - currentUncovered) / universeSize;
                if (coverageRatio < 0.95) return new ArrayList<>(); // Abort
            }

            // Check 4: Stagnation Kill Switch
            // If we haven't improved in 150k iters AND we have >10 holes, give up.
            if (stagnationCounter > 150000 && currentUncovered > 10) {
                return new ArrayList<>();
            }

            int replaceIdx = threadRandom.nextInt(wheelIndices.size());
            int oldCandidateIdx = wheelIndices.get(replaceIdx);

            int newCandidateIdx;
            if (currentUncovered > 0 && threadRandom.nextDouble() < 0.95) {
                int randomHole = findRandomHole(coverageCounts, universeSize, threadRandom);
                if (randomHole != -1) {
                    newCandidateIdx = findCandidateCoveringHole(randomHole, coverageMatrix, usedIndices, candidates.size(), threadRandom);
                } else {
                    newCandidateIdx = findRandomUnusedCandidate(candidates.size(), usedIndices, threadRandom);
                }
            } else {
                newCandidateIdx = findRandomUnusedCandidate(candidates.size(), usedIndices, threadRandom);
            }

            if (newCandidateIdx == -1 || newCandidateIdx == oldCandidateIdx) continue;

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

            boolean accept = energyDiff < 0 || threadRandom.nextDouble() < Math.exp(-energyDiff / temperature);

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
                    if (currentUncovered == 0) break; // Success
                }
            } else {
                stagnationCounter++;
            }

            temperature *= SA_COOLING_RATE;

            if (stagnationCounter > 250000) {
                temperature += 0.5;
                stagnationCounter = 0;
            }
        }

        if (currentUncovered == 0) {
            return bestWheelIndices.stream().map(idx -> bitSetToArray(candidates.get(idx))).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    private List<int[]> matrixBasedGreedySelection(
            List<BitSet> candidates,
            BitSet[] coverageMatrix,
            int universeSize,
            WheelProgressCallback callback,
            AtomicBoolean cancelled) {

        List<int[]> wheel = new ArrayList<>();
        BitSet uncoveredIndices = new BitSet(universeSize);
        uncoveredIndices.set(0, universeSize);

        Set<Integer> usedIndices = new HashSet<>();

        // Parallel stream candidate processing for greedy step can be complex due to state.
        // Keeping linear scan for best Candidate but using fast intersection.

        while (!uncoveredIndices.isEmpty() && !cancelled.get()) {
            int bestIdx = -1;
            int maxCover = -1;

            // This inner loop can be slow if Candidates > 100k.
            // For typical wheels (<20k candidates), this O(N) linear scan is acceptable (~50ms).
            for (int i = 0; i < candidates.size(); i++) {
                if (usedIndices.contains(i)) continue;

                // Fast intersection check
                BitSet coverage = coverageMatrix[i];
                int newCoverCount = countIntersection(coverage, uncoveredIndices);

                if (newCoverCount > maxCover) {
                    maxCover = newCoverCount;
                    bestIdx = i;
                }
            }

            if (bestIdx == -1 || maxCover == 0) break;

            wheel.add(bitSetToArray(candidates.get(bestIdx)));
            usedIndices.add(bestIdx);

            uncoveredIndices.andNot(coverageMatrix[bestIdx]);
        }

        return wheel;
    }

    private int countIntersection(BitSet a, BitSet b) {
        int count = 0;
        for (int i = a.nextSetBit(0); i >= 0; i = a.nextSetBit(i + 1)) {
            if (b.get(i)) count++;
        }
        return count;
    }

    // --- HELPER METHODS ---

    private int findRandomHole(int[] coverageCounts, int universeSize, Random rnd) {
        int start = rnd.nextInt(universeSize);
        for (int i = 0; i < universeSize; i++) {
            int idx = (start + i) % universeSize;
            if (coverageCounts[idx] == 0) return idx;
        }
        return -1;
    }

    private int findCandidateCoveringHole(int holeIdx, BitSet[] coverageMatrix, Set<Integer> usedIndices, int totalCandidates, Random rnd) {
        for(int i=0; i<50; i++) {
            int idx = rnd.nextInt(totalCandidates);
            if (!usedIndices.contains(idx) && coverageMatrix[idx].get(holeIdx)) {
                return idx;
            }
        }
        return -1;
    }

    private int findRandomUnusedCandidate(int candidatesSize, Set<Integer> usedIndices, Random rnd) {
        for(int i=0; i<50; i++) {
            int idx = rnd.nextInt(candidatesSize);
            if(!usedIndices.contains(idx)) return idx;
        }
        return -1;
    }

    private BitSet[] preCalculateCoverageMatrix(List<BitSet> candidates, List<BitSet> universe, int m) {
        BitSet[] matrix = new BitSet[candidates.size()];
        IntStream.range(0, candidates.size()).parallel().forEach(i -> {
            matrix[i] = new BitSet(universe.size());
            BitSet candidate = candidates.get(i);
            for (int j = 0; j < universe.size(); j++) {
                if (hasAtLeastMatches(candidate, universe.get(j), m)) {
                    matrix[i].set(j);
                }
            }
        });
        return matrix;
    }

    private boolean hasAtLeastMatches(BitSet ticket, BitSet draw, int m) {
        int matches = 0;
        for (int i = ticket.nextSetBit(0); i >= 0; i = ticket.nextSetBit(i + 1)) {
            if (draw.get(i)) {
                matches++;
                if (matches >= m) return true;
            }
        }
        return false;
    }

    private int calculateTheoreticalMinimum(WheelParameters params, int universeSize, int candidatesSize) {
        int v = params.getV(); int k = params.getK(); int m = params.getM(); int t = params.getT();
        long coveragePerTicket = 0;
        for (int i = m; i <= k; i++) {
            coveragePerTicket += (long) binomialCoefficient(k, i) * binomialCoefficient(v - k, t - i);
        }
        if (coveragePerTicket == 0) coveragePerTicket = 1;
        return (int) Math.ceil((double) universeSize / coveragePerTicket);
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
        for(int c : coverageCounts) if(c > 0) covered++;
        return covered == universeSize;
    }

    private double binomialCoefficient(int n, int k) {
        if (k > n || k < 0) return 0;
        if (k == 0 || k == n) return 1;
        double result = 1;
        for (int i = 0; i < Math.min(k, n - k); i++) result = result * (n - i) / (i + 1);
        return result;
    }

    private int[] bitSetToArray(BitSet bs) { return bs.stream().toArray(); }
    private BitSet arrayToBitSet(int[] ticket) { BitSet bs = new BitSet(); for (int num : ticket) bs.set(num); return bs; }

    private Set<Set<Integer>> generateUniverse(WheelParameters params, AtomicBoolean cancelled) {
        Set<Set<Integer>> universe = new HashSet<>();
        List<int[]> tCombos = generateAllCombinations(params.getV(), params.getT());
        for (int[] tCombo : tCombos) {
            if (cancelled.get()) break;
            universe.add(arrayToSet(tCombo));
        }
        return universe;
    }

    private List<int[]> generateCandidates(int v, int k) { return generateAllCombinations(v, k); }

    private List<BitSet> convertUniverseToBitSet(Set<Set<Integer>> universe, int v) {
        return universe.stream().map(draw -> {
            BitSet bs = new BitSet(v + 1); for (int num : draw) bs.set(num); return bs;
        }).collect(Collectors.toList());
    }
    private List<BitSet> convertCandidatesToBitSet(List<int[]> candidates, int v) {
        return candidates.stream().map(ticket -> {
            BitSet bs = new BitSet(v + 1); for (int num : ticket) bs.set(num); return bs;
        }).collect(Collectors.toList());
    }

    private List<int[]> generateAllCombinations(int n, int k) {
        List<int[]> result = new ArrayList<>();
        int[] combo = new int[k];
        generateCombinationsRecursive(n, k, 0, 0, combo, result);
        return result;
    }

    private void generateCombinationsRecursive(int n, int k, int start, int index, int[] combo, List<int[]> result) {
        if (index == k) { result.add(combo.clone()); return; }
        for (int i = start; i <= n - k + index; i++) {
            combo[index] = i;
            generateCombinationsRecursive(n, k, i + 1, index + 1, combo, result);
        }
    }

    private Set<Integer> arrayToSet(int[] array) { Set<Integer> set = new HashSet<>(); for (int num : array) set.add(num); return set; }

    // Legacy support methods (empty implementations to satisfy any potential callers, though not used)
    private Map<BitSet, Double> computeRarityWeights(List<BitSet> universe, BitSet[] coverageMatrix) { return new HashMap<>(); }
    private List<int[]> rarityWeightedGreedySelection(List<BitSet> universe, List<BitSet> candidates, Map<BitSet, Double> rarityWeights, WheelParameters params, WheelProgressCallback callback, AtomicBoolean cancelled, int totalRequired) { return new ArrayList<>(); }

}