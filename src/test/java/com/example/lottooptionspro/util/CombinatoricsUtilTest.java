package com.example.lottooptionspro.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinatoricsUtilTest {

    @Test
    void binomial_kZero_returnsOne() {
        assertEquals(1L, CombinatoricsUtil.binomial(10, 0));
        assertEquals(1L, CombinatoricsUtil.binomial(0, 0));
    }

    @Test
    void binomial_kEqualsN_returnsOne() {
        assertEquals(1L, CombinatoricsUtil.binomial(5, 5));
        assertEquals(1L, CombinatoricsUtil.binomial(1, 1));
    }

    @Test
    void binomial_kGreaterThanN_returnsZero() {
        assertEquals(0L, CombinatoricsUtil.binomial(5, 6));
        assertEquals(0L, CombinatoricsUtil.binomial(0, 1));
    }

    @Test
    void binomial_kNegative_returnsZero() {
        assertEquals(0L, CombinatoricsUtil.binomial(5, -1));
    }

    @Test
    void binomial_knownValues() {
        assertEquals(10L, CombinatoricsUtil.binomial(5, 2));
        assertEquals(252L, CombinatoricsUtil.binomial(10, 5));
        assertEquals(658_008L, CombinatoricsUtil.binomial(40, 5));
        assertEquals(76_904_685L, CombinatoricsUtil.binomial(40, 8));
    }

    @Test
    void binomial_symmetric() {
        assertEquals(CombinatoricsUtil.binomial(20, 7), CombinatoricsUtil.binomial(20, 13));
    }

    @Test
    void generateAllCombinations_intInt_countMatchesBinomial() {
        List<int[]> combos = CombinatoricsUtil.generateAllCombinations(6, 3);
        assertEquals(20, combos.size());
        assertEquals(CombinatoricsUtil.binomial(6, 3), combos.size());
    }

    @Test
    void generateAllCombinations_intInt_noDuplicatesRangeOneToN() {
        List<int[]> combos = CombinatoricsUtil.generateAllCombinations(5, 2);
        Set<List<Integer>> unique = combos.stream()
                .map(arr -> Arrays.stream(arr).boxed().collect(Collectors.toList()))
                .collect(Collectors.toSet());
        assertEquals(combos.size(), unique.size());
        for (int[] combo : combos) {
            assertEquals(2, combo.length);
            for (int v : combo) {
                assertTrue(v >= 1 && v <= 5, "value out of range [1..5]: " + v);
            }
        }
    }

    @Test
    void generateAllCombinations_kZero_returnsOneEmptyCombination() {
        List<int[]> combos = CombinatoricsUtil.generateAllCombinations(5, 0);
        assertEquals(1, combos.size());
        assertEquals(0, combos.get(0).length);
    }

    @Test
    void generateAllCombinations_kGreaterThanN_returnsEmpty() {
        assertTrue(CombinatoricsUtil.generateAllCombinations(3, 5).isEmpty());
    }

    @Test
    void generateAllCombinations_fromList_usesSuppliedValues() {
        List<Integer> pool = Arrays.asList(10, 20, 30, 40);
        List<int[]> combos = CombinatoricsUtil.generateAllCombinations(pool, 2);
        assertEquals(CombinatoricsUtil.binomial(4, 2), combos.size());
        for (int[] combo : combos) {
            for (int v : combo) {
                assertTrue(pool.contains(v), "unexpected value: " + v);
            }
        }
    }

    @Test
    void generateAllCombinations_fromList_emptyPoolOrInvalidK_returnsEmpty() {
        assertTrue(CombinatoricsUtil.generateAllCombinations(Collections.emptyList(), 1).isEmpty());
        assertTrue(CombinatoricsUtil.generateAllCombinations(Arrays.asList(1, 2), 3).isEmpty());
        assertTrue(CombinatoricsUtil.generateAllCombinations(null, 1).isEmpty());
    }

    @Test
    void arrayToSet_standardArray() {
        Set<Integer> result = CombinatoricsUtil.arrayToSet(new int[]{1, 2, 3});
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result);
    }

    @Test
    void arrayToSet_empty() {
        assertTrue(CombinatoricsUtil.arrayToSet(new int[0]).isEmpty());
    }

    @Test
    void arrayToSet_null_returnsEmpty() {
        assertTrue(CombinatoricsUtil.arrayToSet(null).isEmpty());
    }

    @Test
    void arrayToSet_deduplicates() {
        Set<Integer> result = CombinatoricsUtil.arrayToSet(new int[]{1, 1, 2, 2, 3});
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result);
    }
}
