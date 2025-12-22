package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AbbreviatedWheelServiceTest {

    private AbbreviatedWheelService sut;

    @BeforeEach
    void setUp() {
        sut = new AbbreviatedWheelService();
    }

    @Test
    @DisplayName("Should generate 4-if-6 wheel for 7 numbers")
    void testGenerateFourIfSixWheel_SevenNumbers_Success() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        WheelConfiguration config = new WheelConfiguration(7, 6, numbers, GuaranteeLevel.FOUR_IF_SIX);

        WheelGenerationResponse result = sut.generateWheel(config);

        assertNotNull(result);
        assertEquals(7, result.getTotalLines());
        assertEquals(GuaranteeLevel.FOUR_IF_SIX, result.getGuarantee());
    }

    @Test
    @DisplayName("Should generate 4-if-6 wheel for 9 numbers")
    void testGenerateFourIfSixWheel_NineNumbers_Success() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        WheelConfiguration config = new WheelConfiguration(9, 6, numbers, GuaranteeLevel.FOUR_IF_SIX);

        WheelGenerationResponse result = sut.generateWheel(config);

        assertNotNull(result);
        assertEquals(13, result.getTotalLines());
        assertEquals(GuaranteeLevel.FOUR_IF_SIX, result.getGuarantee());
        assertNotNull(result.getStatistics());
    }

    @Test
    @DisplayName("Should generate 3-if-5 wheel for 7 numbers")
    void testGenerateThreeIfFiveWheel_SevenNumbers_Success() {
        List<Integer> numbers = Arrays.asList(10, 20, 30, 40, 50, 60, 70);
        WheelConfiguration config = new WheelConfiguration(7, 5, numbers, GuaranteeLevel.THREE_IF_FIVE);

        WheelGenerationResponse result = sut.generateWheel(config);

        assertNotNull(result);
        assertEquals(6, result.getTotalLines());
        assertEquals(GuaranteeLevel.THREE_IF_FIVE, result.getGuarantee());
    }

    @Test
    @DisplayName("Should generate 3-if-5 wheel for 10 numbers")
    void testGenerateThreeIfFiveWheel_TenNumbers_Success() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        WheelConfiguration config = new WheelConfiguration(10, 5, numbers, GuaranteeLevel.THREE_IF_FIVE);

        WheelGenerationResponse result = sut.generateWheel(config);

        assertNotNull(result);
        assertEquals(15, result.getTotalLines());
        assertEquals(GuaranteeLevel.THREE_IF_FIVE, result.getGuarantee());
    }

    @Test
    @DisplayName("Should generate 3-if-4 wheel for Pick-4 game")
    void testGenerateThreeIfFourWheel_Pick4_Success() {
        List<Integer> numbers = Arrays.asList(5, 10, 15, 20, 25, 30, 35);
        WheelConfiguration config = new WheelConfiguration(7, 4, numbers, GuaranteeLevel.THREE_IF_FOUR);

        WheelGenerationResponse result = sut.generateWheel(config);

        assertNotNull(result);
        assertTrue(result.getTotalLines() <= 35);
        assertEquals(GuaranteeLevel.THREE_IF_FOUR, result.getGuarantee());
    }

    @Test
    @DisplayName("Should generate 4-if-5 wheel for Pick-5 game")
    void testGenerateFourIfFiveWheel_Pick5_Success() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        WheelConfiguration config = new WheelConfiguration(8, 5, numbers, GuaranteeLevel.FOUR_IF_FIVE_P5);

        WheelGenerationResponse result = sut.generateWheel(config);

        assertNotNull(result);
        assertTrue(result.getTotalLines() <= 56);
        assertEquals(GuaranteeLevel.FOUR_IF_FIVE_P5, result.getGuarantee());
    }

    @Test
    @DisplayName("Should handle medium pool size of 15 numbers")
    void testGenerateWheel_MediumPoolSize_Success() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            numbers.add(i);
        }
        WheelConfiguration config = new WheelConfiguration(15, 6, numbers, GuaranteeLevel.FOUR_IF_FOUR);

        WheelGenerationResponse result = sut.generateWheel(config);

        assertNotNull(result);
        assertTrue(result.getTotalLines() > 0);
        assertEquals(GuaranteeLevel.FOUR_IF_FOUR, result.getGuarantee());
    }

    @Test
    @DisplayName("Should throw exception for pool size less than 4")
    void testGenerateWheel_PoolSizeTooSmall_ThrowsException() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);
        WheelConfiguration config = new WheelConfiguration(3, 6, numbers, GuaranteeLevel.FOUR_IF_SIX);

        assertThrows(IllegalArgumentException.class, () -> sut.generateWheel(config));
    }

    @Test
    @DisplayName("Should throw exception for pool size greater than 27")
    void testGenerateWheel_PoolSizeTooLarge_ThrowsException() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 28; i++) {
            numbers.add(i);
        }
        WheelConfiguration config = new WheelConfiguration(28, 6, numbers, GuaranteeLevel.FOUR_IF_SIX);

        assertThrows(IllegalArgumentException.class, () -> sut.generateWheel(config));
    }

    @Test
    @DisplayName("Should throw exception for invalid pick size")
    void testGenerateWheel_InvalidPickSize_ThrowsException() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        WheelConfiguration config = new WheelConfiguration(7, 7, numbers, GuaranteeLevel.FOUR_IF_SIX);

        assertThrows(IllegalArgumentException.class, () -> sut.generateWheel(config));
    }

    @Test
    @DisplayName("Should throw exception for mismatched guarantee level and pick size")
    void testGenerateWheel_MismatchedGuaranteeLevel_ThrowsException() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        WheelConfiguration config = new WheelConfiguration(7, 5, numbers, GuaranteeLevel.FOUR_IF_SIX);

        assertThrows(IllegalArgumentException.class, () -> sut.generateWheel(config));
    }

    @Test
    @DisplayName("Should estimate line count accurately")
    void testEstimateLineCount_Success() {
        long result = sut.estimateLineCount(7, 6, GuaranteeLevel.FOUR_IF_SIX);

        assertEquals(7, result);
    }

    @Test
    @DisplayName("Should generate all combinations sorted")
    void testGenerateWheel_CombinationsSorted_Success() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        WheelConfiguration config = new WheelConfiguration(7, 6, numbers, GuaranteeLevel.FOUR_IF_SIX);

        WheelGenerationResponse result = sut.generateWheel(config);

        for (int[] combination : result.getCombinations()) {
            for (int i = 0; i < combination.length - 1; i++) {
                assertTrue(combination[i] < combination[i + 1], "Combinations should be sorted");
            }
        }
    }

    @Test
    @DisplayName("Should generate unique combinations")
    void testGenerateWheel_UniqueCombinations_Success() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        WheelConfiguration config = new WheelConfiguration(7, 6, numbers, GuaranteeLevel.FOUR_IF_SIX);

        WheelGenerationResponse result = sut.generateWheel(config);

        Set<String> uniqueCombos = new HashSet<>();
        for (int[] combo : result.getCombinations()) {
            String comboStr = Arrays.toString(combo);
            assertFalse(uniqueCombos.contains(comboStr), "Duplicate combination found: " + comboStr);
            uniqueCombos.add(comboStr);
        }
    }

    @Test
    @DisplayName("Should calculate statistics correctly")
    void testGenerateWheel_StatisticsCalculated_Success() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        WheelConfiguration config = new WheelConfiguration(7, 6, numbers, GuaranteeLevel.FOUR_IF_SIX);

        WheelGenerationResponse result = sut.generateWheel(config);

        WheelStatistics stats = result.getStatistics();
        assertNotNull(stats);
        assertEquals(7, stats.getTotalCombinations());
        assertEquals(7, stats.getPoolSize());
        assertEquals(6, stats.getPickSize());
        assertEquals("4-if-6", stats.getGuarantee());
        assertTrue(stats.getCoveragePercentage() > 0 && stats.getCoveragePercentage() <= 100);
    }

    @Test
    @DisplayName("Should get available guarantees for Pick-6")
    void testGetAvailableGuarantees_Pick6_Success() {
        GuaranteeLevel[] result = GuaranteeLevel.getAvailableGuaranteesForPickSize(6);

        assertEquals(4, result.length);
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.FIVE_IF_SIX));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.FOUR_IF_SIX));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.FOUR_IF_FIVE));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.FOUR_IF_FOUR));
    }

    @Test
    @DisplayName("Should get available guarantees for Pick-5")
    void testGetAvailableGuarantees_Pick5_Success() {
        GuaranteeLevel[] result = GuaranteeLevel.getAvailableGuaranteesForPickSize(5);

        assertEquals(4, result.length);
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.FOUR_IF_FIVE_P5));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.FOUR_IF_FOUR_P5));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.THREE_IF_FIVE));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.THREE_IF_FOUR_P5));
    }

    @Test
    @DisplayName("Should get available guarantees for Pick-4")
    void testGetAvailableGuarantees_Pick4_Success() {
        GuaranteeLevel[] result = GuaranteeLevel.getAvailableGuaranteesForPickSize(4);

        assertEquals(4, result.length);
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.THREE_IF_FOUR));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.THREE_IF_THREE));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.TWO_IF_FOUR));
        assertTrue(Arrays.asList(result).contains(GuaranteeLevel.TWO_IF_THREE));
    }

}
