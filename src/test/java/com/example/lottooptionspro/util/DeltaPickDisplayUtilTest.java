package com.example.lottooptionspro.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeltaPickDisplayUtil following AAA pattern.
 */
class DeltaPickDisplayUtilTest {

    @Test
    void testFormatNumbersArray_ValidList() {
        // Arrange
        List<Integer> numbers = Arrays.asList(3, 6, 9, 16, 29);

        // Act
        String result = DeltaPickDisplayUtil.formatNumbersArray(numbers);

        // Assert
        assertEquals("3-6-9-16-29", result, "Should format numbers with hyphens");
    }

    @Test
    void testFormatNumbersArray_SingleNumber() {
        // Arrange
        List<Integer> numbers = Collections.singletonList(5);

        // Act
        String result = DeltaPickDisplayUtil.formatNumbersArray(numbers);

        // Assert
        assertEquals("5", result, "Should handle single number");
    }

    @Test
    void testFormatNumbersArray_EmptyList() {
        // Arrange
        List<Integer> numbers = Collections.emptyList();

        // Act
        String result = DeltaPickDisplayUtil.formatNumbersArray(numbers);

        // Assert
        assertEquals("", result, "Should return empty string for empty list");
    }

    @Test
    void testFormatNumbersArray_NullList() {
        // Arrange
        List<Integer> numbers = null;

        // Act
        String result = DeltaPickDisplayUtil.formatNumbersArray(numbers);

        // Assert
        assertEquals("", result, "Should return empty string for null list");
    }

    @Test
    void testFormatProbabilityAsPercentage_ValidScore() {
        // Arrange
        Double probabilityScore = 0.002246999347118714;

        // Act
        String result = DeltaPickDisplayUtil.formatProbabilityAsPercentage(probabilityScore);

        // Assert
        assertEquals("0.22%", result, "Should format as percentage with 2 decimals");
    }

    @Test
    void testFormatProbabilityAsPercentage_HighScore() {
        // Arrange
        Double probabilityScore = 0.5;

        // Act
        String result = DeltaPickDisplayUtil.formatProbabilityAsPercentage(probabilityScore);

        // Assert
        assertEquals("50.00%", result, "Should format 0.5 as 50.00%");
    }

    @Test
    void testFormatProbabilityAsPercentage_NullScore() {
        // Arrange
        Double probabilityScore = null;

        // Act
        String result = DeltaPickDisplayUtil.formatProbabilityAsPercentage(probabilityScore);

        // Assert
        assertEquals("0.00%", result, "Should return 0.00% for null");
    }

    @Test
    void testFormatProbabilityScientific_ValidScore() {
        // Arrange
        Double probabilityScore = 0.002246999347118714;

        // Act
        String result = DeltaPickDisplayUtil.formatProbabilityScientific(probabilityScore);

        // Assert
        assertTrue(result.matches("\\d\\.\\d{2}e[+-]?\\d+"), "Should be in scientific notation");
        assertTrue(result.contains("e"), "Should contain 'e' for scientific notation");
    }

    @Test
    void testFormatProbabilityScientific_NullScore() {
        // Arrange
        Double probabilityScore = null;

        // Act
        String result = DeltaPickDisplayUtil.formatProbabilityScientific(probabilityScore);

        // Assert
        assertEquals("0.00e0", result, "Should return 0.00e0 for null");
    }

    @Test
    void testFormatExecutionTime_Milliseconds() {
        // Arrange
        Integer executionTimeMs = 60;

        // Act
        String result = DeltaPickDisplayUtil.formatExecutionTime(executionTimeMs);

        // Assert
        assertEquals("60ms", result, "Should format as milliseconds");
    }

    @Test
    void testFormatExecutionTime_Seconds() {
        // Arrange
        Integer executionTimeMs = 1500;

        // Act
        String result = DeltaPickDisplayUtil.formatExecutionTime(executionTimeMs);

        // Assert
        assertEquals("1.5s", result, "Should format as seconds");
    }

    @Test
    void testFormatExecutionTime_ExactlyOneSecond() {
        // Arrange
        Integer executionTimeMs = 1000;

        // Act
        String result = DeltaPickDisplayUtil.formatExecutionTime(executionTimeMs);

        // Assert
        assertEquals("1.0s", result, "Should format 1000ms as 1.0s");
    }

    @Test
    void testFormatExecutionTime_Zero() {
        // Arrange
        Integer executionTimeMs = 0;

        // Act
        String result = DeltaPickDisplayUtil.formatExecutionTime(executionTimeMs);

        // Assert
        assertEquals("0ms", result, "Should return 0ms for zero");
    }

    @Test
    void testFormatExecutionTime_Null() {
        // Arrange
        Integer executionTimeMs = null;

        // Act
        String result = DeltaPickDisplayUtil.formatExecutionTime(executionTimeMs);

        // Assert
        assertEquals("0ms", result, "Should return 0ms for null");
    }

    @Test
    void testFormatNumberWithCommas_SmallNumber() {
        // Arrange
        Integer number = 100;

        // Act
        String result = DeltaPickDisplayUtil.formatNumberWithCommas(number);

        // Assert
        assertEquals("100", result, "Should not add commas for numbers < 1000");
    }

    @Test
    void testFormatNumberWithCommas_LargeNumber() {
        // Arrange
        Integer number = 20740;

        // Act
        String result = DeltaPickDisplayUtil.formatNumberWithCommas(number);

        // Assert
        assertEquals("20,740", result, "Should add comma separator");
    }

    @Test
    void testFormatNumberWithCommas_VeryLargeNumber() {
        // Arrange
        Integer number = 1234567;

        // Act
        String result = DeltaPickDisplayUtil.formatNumberWithCommas(number);

        // Assert
        assertEquals("1,234,567", result, "Should add multiple comma separators");
    }

    @Test
    void testFormatNumberWithCommas_Null() {
        // Arrange
        Integer number = null;

        // Act
        String result = DeltaPickDisplayUtil.formatNumberWithCommas(number);

        // Assert
        assertEquals("0", result, "Should return 0 for null");
    }
}
