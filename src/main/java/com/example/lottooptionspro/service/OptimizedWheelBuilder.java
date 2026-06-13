package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import com.example.lottooptionspro.util.CombinatoricsUtil;
import jakarta.annotation.PreDestroy;
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

    private static final double REDUNDANCY_PENALTY = 0.0001;
    private static final int SESSION_CACHE_MAX_ENTRIES = 50;
    private final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private final Map<String, List<int[]>> sessionCache = Collections.synchronizedMap(
            new LinkedHashMap<String, List<int[]>>(SESSION_CACHE_MAX_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<int[]>> eldest) {
                    return size() > SESSION_CACHE_MAX_ENTRIES;
                }
            });

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public List<int[]> buildOptimalWheel(WheelParameters params, WheelProgressCallback callback, AtomicBoolean cancelled) {
        params.validate();

        String cacheKey = params.getNotation();
        if (sessionCache.containsKey(cacheKey)) {
            logger.info("Returning cached wheel for {}", cacheKey);
            callback.update("Found in cache!", 1.0, 0, 0, sessionCache.get(cacheKey).size());
            return sessionCache.get(cacheKey);
        }

        logger.info("Building optimal wheel for {}", cacheKey);

        // --- Setup Phase ---
        callback.update("Analyzing coverage...", 0.05, 0, 0, 0);
        Set<Set<Integer>> universe = generateUniverse(params, cancelled);
        if (cancelled.get()) return new ArrayList<>();
        int totalRequired = universe.size();

        callback.update("Generating candidates...", 0.1, 0, totalRequired, 0);
        List<int[]> candidates = generateCandidates(params.getV(), params.getK());

        List<BitSet> universeBitSet = convertUniverseToBitSet(universe, params.getV());
        List<BitSet> candidatesBitSet = convertCandidatesToBitSet(candidates, params.getV());

        callback.update("Building Search Indices...", 0.15, 0, totalRequired, 0);
        BitSet[] coverageMatrix = preCalculateCoverageMatrix(candidatesBitSet, universeBitSet, params.getM());
        int[][] reverseCoverage = preCalculateReverseMatrix(coverageMatrix, totalRequired, candidates.size());
        double[] itemWeights = calculateItemWeights(reverseCoverage, totalRequired);

        // 1. Establish Upper Limit (Greedy)
        callback.update("Establishing Upper Limit...", 0.2, 0, totalRequired, 0);
        List<int[]> greedyWheel = runMultiThreadedWeightedGreedy(candidatesBitSet, coverageMatrix, itemWeights, totalRequired);
        int ceiling = greedyWheel.size();
        logger.info("Greedy Upper Limit: {} lines", ceiling);

        // 2. Run Hybrid Top-Down Optimization
        int theoreticalMin = calculateTheoreticalMinimum(params, totalRequired, candidates.size());
        callback.update("Running Hybrid Optimization...", 0.5, totalRequired, totalRequired, ceiling);

        List<int[]> bestWheel = runHybridTopDownSearch(
                greedyWheel, theoreticalMin,
                universeBitSet, candidatesBitSet,
                coverageMatrix, reverseCoverage,
                callback, cancelled
        );

        sessionCache.put(cacheKey, bestWheel);
        callback.update("Complete!", 1.0, totalRequired, totalRequired, bestWheel.size());
        logger.info("Final wheel size: {} lines", bestWheel.size());

        return bestWheel;
    }

    /**
     * HYBRID TOP-DOWN SEARCH
     * Prunes downwards from the Greedy Baseline.
     * Uses a mix of Warm Start (Pruning) and Cold Start (Fresh) threads for maximum robustness.
     */
    private List<int[]> runHybridTopDownSearch(
            List<int[]> startingWheel,
            int theoreticalMin,
            List<BitSet> universe,
            List<BitSet> candidates,
            BitSet[] coverageMatrix,
            int[][] reverseCoverage,
            WheelProgressCallback callback,
            AtomicBoolean cancelled) {

        List<int[]> currentBestWheel = new ArrayList<>(startingWheel);
        int currentSize = currentBestWheel.size();

        Map<BitSet, Integer> candidateIndexMap = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) candidateIndexMap.put(candidates.get(i), i);

        logger.info("Hybrid Search Strategy: Pruning {} -> {}", currentSize, theoreticalMin);

        // Loop downwards: 48 -> 47 -> 46 ...
        while (currentSize > theoreticalMin && !cancelled.get()) {
            int targetSize = currentSize - 1;

            double progress = 1.0 - ((double)(targetSize - theoreticalMin) / Math.max(1, startingWheel.size() - theoreticalMin));
            callback.update(String.format("Optimizing %d tickets...", targetSize), 0.5 + (0.5 * progress),
                    universe.size(), universe.size(), targetSize);

            // Run HYBRID Race: Some threads Prune, some threads Cold Start
            List<int[]> result = runHybridRace(
                    targetSize, currentBestWheel,
                    universe, candidates, coverageMatrix, reverseCoverage,
                    candidateIndexMap,
                    60000, // 60 Seconds per step
                    cancelled
            );

            if (result != null && !result.isEmpty()) {
                logger.info("SUCCESS: Reduced to {} lines!", targetSize);
                currentBestWheel = result;
                currentSize = targetSize;
            } else {
                logger.info("Stuck at {} lines. Stopping optimization.", currentSize);
                break; // We hit the hard limit for 60s search
            }
        }

        return currentBestWheel;
    }

    private List<int[]> runHybridRace(
            int targetSize,
            List<int[]> warmStartWheel,
            List<BitSet> universe,
            List<BitSet> candidates,
            BitSet[] coverageMatrix,
            int[][] reverseCoverage,
            Map<BitSet, Integer> candidateIndexMap,
            long timeoutMs,
            AtomicBoolean cancelled) {

        ExecutorCompletionService<List<int[]>> completionService = new ExecutorCompletionService<>(executor);
        List<Future<List<int[]>>> futures = new ArrayList<>();

        // Prepare Warm Start Indices
        List<Integer> warmStartIndices = null;
        if (warmStartWheel != null) {
            warmStartIndices = warmStartWheel.stream()
                    .map(ticket -> candidateIndexMap.get(arrayToBitSet(ticket)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        final List<Integer> finalWarmIndices = warmStartIndices;

        // --- SPLIT STRATEGY ---
        // 40% Threads = Warm Start (Try to prune existing)
        // 60% Threads = Cold Start (Try to build fresh)
        int warmThreads = (int)(THREAD_COUNT * 0.4);

        for (int i = 0; i < THREAD_COUNT; i++) {
            boolean useWarmStart = (i < warmThreads) && (finalWarmIndices != null);
            List<Integer> startIndices = useWarmStart ? finalWarmIndices : null;

            int maxIterations = useWarmStart ? 5_000_000 : 50_000_000;

            futures.add(completionService.submit(() ->
                    runFixedSizeSA(
                            targetSize, startIndices, // Pass mixed strategies
                            universe, candidates, coverageMatrix, reverseCoverage, cancelled,
                            maxIterations, timeoutMs + System.currentTimeMillis()
                    )
            ));
        }

        List<int[]> winner = null;
        int finishers = 0;

        try {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (finishers < THREAD_COUNT) {
                long timeLeft = deadline - System.currentTimeMillis();
                if (timeLeft <= 0) break;
                Future<List<int[]>> future = completionService.poll(timeLeft, TimeUnit.MILLISECONDS);
                if (future == null) break;
                finishers++;
                List<int[]> res = future.get();
                if (res != null && !res.isEmpty()) {
                    if (verifyWheelCoverage(res, candidateIndexMap, coverageMatrix, universe.size())) {
                        winner = res;
                        break;
                    }
                }
            }
        } catch (Exception e) { logger.error("Race error", e); }

        for (Future<?> f : futures) f.cancel(true);
        return winner;
    }

    // --- (Keep runFixedSizeSA, runMultiThreadedWeightedGreedy, and all helpers from previous solution) ---
    // Copy the exact `runFixedSizeSA`, `weightedGreedy`, `preCalculate...` methods here.
    // They are unchanged, only the Search Strategy logic above is new.

    // ... [Boilerplate to make this compile-ready if copied directly] ...

    private List<int[]> runFixedSizeSA(
            int targetSize,
            List<Integer> warmStartIndices,
            List<BitSet> universe,
            List<BitSet> candidates,
            BitSet[] coverageMatrix,
            int[][] reverseCoverage,
            AtomicBoolean cancelled,
            int maxIterations,
            long deadlineTimestamp) {

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Integer> wheelIndices;
        int universeSize = universe.size();

        if (warmStartIndices != null && warmStartIndices.size() >= targetSize) {
            wheelIndices = new ArrayList<>(warmStartIndices);
            while (wheelIndices.size() > targetSize) wheelIndices.remove(rnd.nextInt(wheelIndices.size()));
        } else {
            wheelIndices = new ArrayList<>();
            Set<Integer> used = new HashSet<>();
            while (wheelIndices.size() < targetSize) {
                int c = rnd.nextInt(candidates.size());
                if (used.add(c)) wheelIndices.add(c);
            }
        }

        int[] coverageCounts = new int[universeSize];
        int currentUncovered = universeSize;
        int currentRedundancy = 0;
        Set<Integer> usedIndices = new HashSet<>(wheelIndices);

        for (int idx : wheelIndices) {
            BitSet coverage = coverageMatrix[idx];
            for (int drawIdx = coverage.nextSetBit(0); drawIdx >= 0; drawIdx = coverage.nextSetBit(drawIdx + 1)) {
                if (coverageCounts[drawIdx] == 0) currentUncovered--;
                if (coverageCounts[drawIdx] == 1) currentRedundancy++;
                coverageCounts[drawIdx]++;
            }
        }

        double currentEnergy = currentUncovered + (REDUNDANCY_PENALTY * currentRedundancy);
        boolean isColdStart = (warmStartIndices == null);
        double temperature = isColdStart ? 5.0 : 1.5;
        double coolingRate = isColdStart ? 0.999999 : 0.999995;

        for (int iter = 0; iter < maxIterations; iter++) {
            if (iter % 1000 == 0) {
                if (System.currentTimeMillis() > deadlineTimestamp || cancelled.get()) return null;
            }

            int replaceIdx = rnd.nextInt(wheelIndices.size());
            int oldCandidateIdx = wheelIndices.get(replaceIdx);
            int newCandidateIdx = -1;

            if (currentUncovered > 0 && rnd.nextDouble() < 0.95) {
                int randomHole = findRandomHole(coverageCounts, universeSize, rnd);
                if (randomHole != -1) {
                    int[] helpers = reverseCoverage[randomHole];
                    if (helpers.length > 0) {
                        int bestCandidate = -1;
                        int bestImprovement = Integer.MIN_VALUE;
                        for (int k = 0; k < 5; k++) {
                            int candidate = helpers[rnd.nextInt(helpers.length)];
                            if (usedIndices.contains(candidate)) continue;
                            BitSet newCov = coverageMatrix[candidate];
                            int improvement = 0;
                            for (int i = newCov.nextSetBit(0); i >= 0; i = newCov.nextSetBit(i + 1)) {
                                if (coverageCounts[i] == 0) improvement++;
                            }
                            if (improvement > bestImprovement) {
                                bestImprovement = improvement;
                                bestCandidate = candidate;
                            }
                        }
                        newCandidateIdx = (bestCandidate != -1) ? bestCandidate : findRandomUnusedCandidate(candidates.size(), usedIndices, rnd);
                    } else {
                        newCandidateIdx = findRandomUnusedCandidate(candidates.size(), usedIndices, rnd);
                    }
                } else {
                    newCandidateIdx = findRandomUnusedCandidate(candidates.size(), usedIndices, rnd);
                }
            } else {
                newCandidateIdx = findRandomUnusedCandidate(candidates.size(), usedIndices, rnd);
            }

            if (newCandidateIdx == -1 || newCandidateIdx == oldCandidateIdx) continue;

            int deltaUncovered = 0;
            int deltaRedundancy = 0;
            BitSet oldCov = coverageMatrix[oldCandidateIdx];
            for (int i = oldCov.nextSetBit(0); i >= 0; i = oldCov.nextSetBit(i + 1)) {
                if (coverageCounts[i] == 1) deltaUncovered++;
                if (coverageCounts[i] == 2) deltaRedundancy--;
            }
            BitSet newCov = coverageMatrix[newCandidateIdx];
            for (int i = newCov.nextSetBit(0); i >= 0; i = newCov.nextSetBit(i + 1)) {
                int countAfter = coverageCounts[i] - (oldCov.get(i) ? 1 : 0);
                if (countAfter == 0) deltaUncovered--;
                if (countAfter == 1) deltaRedundancy++;
            }

            double newEnergy = (currentUncovered + deltaUncovered) + (REDUNDANCY_PENALTY * (currentRedundancy + deltaRedundancy));
            double energyDiff = newEnergy - currentEnergy;

            if (energyDiff < 0 || rnd.nextDouble() < Math.exp(-energyDiff / temperature)) {
                wheelIndices.set(replaceIdx, newCandidateIdx);
                usedIndices.remove(oldCandidateIdx); usedIndices.add(newCandidateIdx);
                for (int i = oldCov.nextSetBit(0); i >= 0; i = oldCov.nextSetBit(i + 1)) coverageCounts[i]--;
                for (int i = newCov.nextSetBit(0); i >= 0; i = newCov.nextSetBit(i + 1)) coverageCounts[i]++;
                currentUncovered += deltaUncovered; currentRedundancy += deltaRedundancy;
                currentEnergy = newEnergy;

                if (currentUncovered == 0) {
                    return wheelIndices.stream().map(idx -> bitSetToArray(candidates.get(idx))).collect(Collectors.toList());
                }
            }
            temperature *= coolingRate;
        }
        return null;
    }

    // --- REUSED HELPERS ---
    private List<int[]> runMultiThreadedWeightedGreedy(List<BitSet> candidates, BitSet[] coverageMatrix, double[] itemWeights, int universeSize) {
        ExecutorCompletionService<List<int[]>> completionService = new ExecutorCompletionService<>(executor);
        int threadCount = THREAD_COUNT;
        for (int i = 0; i < threadCount; i++) {
            completionService.submit(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                List<Integer> indices = IntStream.range(0, candidates.size()).boxed().collect(Collectors.toList());
                List<int[]> localBestWheel = null;
                int localBestSize = Integer.MAX_VALUE;
                long deadline = System.currentTimeMillis() + 10000;
                while (System.currentTimeMillis() < deadline && !Thread.currentThread().isInterrupted()) {
                    Collections.shuffle(indices, rnd);
                    List<int[]> wheel = weightedGreedy(candidates, indices, coverageMatrix, itemWeights, universeSize);
                    if (wheel.size() < localBestSize) {
                        localBestSize = wheel.size();
                        localBestWheel = wheel;
                    }
                }
                return localBestWheel;
            });
        }
        List<int[]> globalBestWheel = null;
        int globalBestSize = Integer.MAX_VALUE;
        int finished = 0;
        try {
            while (finished < threadCount) {
                Future<List<int[]>> future = completionService.take();
                List<int[]> wheel = future.get();
                if (wheel != null && wheel.size() < globalBestSize) {
                    globalBestSize = wheel.size();
                    globalBestWheel = wheel;
                    logger.info("New Record found: {} lines", globalBestSize);
                }
                finished++;
            }
        } catch (Exception e) { logger.error("Greedy error", e); }
        return globalBestWheel;
    }

    private List<int[]> weightedGreedy(List<BitSet> candidates, List<Integer> indices, BitSet[] matrix, double[] itemWeights, int uSize) {
        List<int[]> wheel = new ArrayList<>();
        BitSet uncovered = new BitSet(uSize); uncovered.set(0, uSize);
        BitSet used = new BitSet(candidates.size());
        while(!uncovered.isEmpty()) {
            int bestIdx = -1; double maxScore = -1;
            for(Integer idx : indices) {
                if(used.get(idx)) continue;
                BitSet m = matrix[idx];
                double score = 0;
                for(int bit=m.nextSetBit(0); bit>=0; bit=m.nextSetBit(bit+1)) {
                    if(uncovered.get(bit)) score += itemWeights[bit];
                }
                if(score > maxScore) { maxScore = score; bestIdx = idx; }
            }
            if(bestIdx == -1 || maxScore == 0) break;
            wheel.add(bitSetToArray(candidates.get(bestIdx)));
            used.set(bestIdx);
            uncovered.andNot(matrix[bestIdx]);
        }
        return wheel;
    }

    private double[] calculateItemWeights(int[][] reverseCoverage, int universeSize) {
        double[] weights = new double[universeSize];
        for(int i=0; i<universeSize; i++) {
            int count = reverseCoverage[i].length;
            weights[i] = (count > 0) ? (1.0 / count) : 1000.0;
        }
        return weights;
    }

    private int[][] preCalculateReverseMatrix(BitSet[] coverageMatrix, int universeSize, int candidatesCount) {
        List<Integer>[] temp = new ArrayList[universeSize];
        for(int i=0; i<universeSize; i++) temp[i] = new ArrayList<>();
        IntStream.range(0, candidatesCount).parallel().forEach(cIdx -> {
            BitSet cov = coverageMatrix[cIdx];
            for (int drawIdx = cov.nextSetBit(0); drawIdx >= 0; drawIdx = cov.nextSetBit(drawIdx + 1)) {
                synchronized(temp[drawIdx]) { temp[drawIdx].add(cIdx); }
            }
        });
        int[][] reverse = new int[universeSize][];
        for(int i=0; i<universeSize; i++) reverse[i] = temp[i].stream().mapToInt(Integer::intValue).toArray();
        return reverse;
    }

    // --- STANDARD HELPERS ---
    private int findRandomHole(int[] coverageCounts, int universeSize, Random rnd) {
        int start = rnd.nextInt(universeSize);
        for (int i = 0; i < universeSize; i++) { int idx = (start+i)%universeSize; if(coverageCounts[idx]==0) return idx; } return -1;
    }
    private int findRandomUnusedCandidate(int total, Set<Integer> used, Random rnd) {
        for(int i=0; i<50; i++) { int idx = rnd.nextInt(total); if(!used.contains(idx)) return idx; } return -1;
    }
    private BitSet[] preCalculateCoverageMatrix(List<BitSet> candidates, List<BitSet> universe, int m) {
        BitSet[] matrix = new BitSet[candidates.size()];
        IntStream.range(0, candidates.size()).parallel().forEach(i -> {
            matrix[i] = new BitSet(universe.size());
            BitSet candidate = candidates.get(i);
            for (int j = 0; j < universe.size(); j++) {
                if (hasAtLeastMatches(candidate, universe.get(j), m)) matrix[i].set(j);
            }
        });
        return matrix;
    }
    private boolean hasAtLeastMatches(BitSet ticket, BitSet draw, int m) {
        int matches = 0;
        for (int i = ticket.nextSetBit(0); i >= 0; i = ticket.nextSetBit(i + 1)) {
            if (draw.get(i)) { matches++; if (matches >= m) return true; }
        }
        return false;
    }
    private int calculateTheoreticalMinimum(WheelParameters params, int uSize, int cSize) {
        int v = params.getV(); int k = params.getK(); int m = params.getM(); int t = params.getT();
        long cov = 0;
        for (int i = m; i <= k; i++) {
            cov += CombinatoricsUtil.binomial(k, i) * CombinatoricsUtil.binomial(v - k, t - i);
        }
        return (int) Math.ceil((double) uSize / Math.max(1, cov));
    }
    private boolean verifyWheelCoverage(List<int[]> wheel, Map<BitSet, Integer> map, BitSet[] matrix, int uSize) {
        int[] counts = new int[uSize];
        for(int[] t : wheel) { Integer idx = map.get(arrayToBitSet(t)); if(idx!=null) { BitSet c = matrix[idx]; for(int i=c.nextSetBit(0); i>=0; i=c.nextSetBit(i+1)) counts[i]++; }}
        for(int c : counts) if(c==0) return false; return true;
    }
    private int[] bitSetToArray(BitSet bs) { return bs.stream().toArray(); }
    private BitSet arrayToBitSet(int[] ticket) { BitSet bs = new BitSet(); for (int num : ticket) bs.set(num); return bs; }
    private Set<Set<Integer>> generateUniverse(WheelParameters params, AtomicBoolean cancelled) {
        Set<Set<Integer>> u = new HashSet<>();
        List<int[]> c = zeroBasedCombinations(params.getV(), params.getT());
        for (int[] i : c) { if (cancelled.get()) break; u.add(CombinatoricsUtil.arrayToSet(i)); }
        return u;
    }
    private List<int[]> generateCandidates(int v, int k) { return zeroBasedCombinations(v, k); }
    private List<BitSet> convertUniverseToBitSet(Set<Set<Integer>> u, int v) { return u.stream().map(d -> { BitSet bs = new BitSet(v+1); for(int i:d) bs.set(i); return bs; }).collect(Collectors.toList()); }
    private List<BitSet> convertCandidatesToBitSet(List<int[]> c, int v) { return c.stream().map(t -> { BitSet bs = new BitSet(v+1); for(int i:t) bs.set(i); return bs; }).collect(Collectors.toList()); }

    private static List<int[]> zeroBasedCombinations(int n, int k) {
        return CombinatoricsUtil.generateAllCombinations(
                IntStream.range(0, n).boxed().collect(Collectors.toList()), k);
    }
}