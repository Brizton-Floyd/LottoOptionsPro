package com.example.lottooptionspro.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CombinatoricsUtil {

    private CombinatoricsUtil() {
    }

    public static long binomial(int n, int k) {
        if (k < 0 || k > n) {
            return 0L;
        }
        if (k == 0 || k == n) {
            return 1L;
        }
        int kEffective = Math.min(k, n - k);
        long result = 1L;
        for (int i = 0; i < kEffective; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    public static List<int[]> generateAllCombinations(int n, int k) {
        List<int[]> combinations = new ArrayList<>();
        if (k < 0 || k > n) {
            return combinations;
        }
        int[] current = new int[k];
        generateRecursive(1, n, k, 0, current, combinations);
        return combinations;
    }

    public static List<int[]> generateAllCombinations(List<Integer> numbers, int k) {
        List<int[]> combinations = new ArrayList<>();
        if (numbers == null || k < 0 || k > numbers.size()) {
            return combinations;
        }
        int[] pool = new int[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            pool[i] = numbers.get(i);
        }
        int[] current = new int[k];
        generateRecursiveFromPool(pool, 0, k, 0, current, combinations);
        return combinations;
    }

    public static Set<Integer> arrayToSet(int[] array) {
        Set<Integer> result = new HashSet<>();
        if (array == null) {
            return result;
        }
        for (int value : array) {
            result.add(value);
        }
        return result;
    }

    private static void generateRecursive(int start, int n, int k, int depth,
                                          int[] current, List<int[]> combinations) {
        if (depth == k) {
            combinations.add(current.clone());
            return;
        }
        for (int i = start; i <= n - (k - depth) + 1; i++) {
            current[depth] = i;
            generateRecursive(i + 1, n, k, depth + 1, current, combinations);
        }
    }

    private static void generateRecursiveFromPool(int[] pool, int start, int k, int depth,
                                                  int[] current, List<int[]> combinations) {
        if (depth == k) {
            combinations.add(current.clone());
            return;
        }
        for (int i = start; i <= pool.length - (k - depth); i++) {
            current[depth] = pool[i];
            generateRecursiveFromPool(pool, i + 1, k, depth + 1, current, combinations);
        }
    }
}
