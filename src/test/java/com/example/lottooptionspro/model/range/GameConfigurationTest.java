package com.example.lottooptionspro.model.range;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigurationTest {

    @Test
    void testPowerballConfiguration() {
        GameConfiguration config = new GameConfiguration("CA", "Powerball");
        
        assertEquals("CA", config.getState());
        assertEquals("Powerball", config.getGame());
        assertEquals(1, config.getMinNumber());
        assertEquals(69, config.getMaxNumber());
        assertEquals(5, config.getNumbersDrawn());
        assertEquals(1, config.getBonusNumbers());
        assertTrue(config.hasBonus());
        assertEquals("1-69", config.getNumberRangeString());
        assertEquals(10, config.getDefaultRangeSize());
        assertEquals(100, config.getRecommendedMaxDraws());
        
        List<Integer> drawPositions = config.getDrawPositions();
        assertEquals(List.of(1, 2, 3, 4, 5), drawPositions);
    }
    
    @Test
    void testMegaMillionsConfiguration() {
        GameConfiguration config = new GameConfiguration("FL", "Mega Millions");
        
        assertEquals(1, config.getMinNumber());
        assertEquals(70, config.getMaxNumber());
        assertEquals(5, config.getNumbersDrawn());
        assertEquals(1, config.getBonusNumbers());
        assertEquals(10, config.getDefaultRangeSize());
        assertEquals(100, config.getRecommendedMaxDraws());
    }
    
    @Test
    void testPick4Configuration() {
        GameConfiguration config = new GameConfiguration("TX", "Pick4");
        
        assertEquals(0, config.getMinNumber());
        assertEquals(9, config.getMaxNumber());
        assertEquals(4, config.getNumbersDrawn());
        assertEquals(0, config.getBonusNumbers());
        assertFalse(config.hasBonus());
        assertEquals("0-9", config.getNumberRangeString());
        assertEquals(2, config.getDefaultRangeSize());
        assertEquals(40, config.getRecommendedMaxDraws());
        
        List<Integer> drawPositions = config.getDrawPositions();
        assertEquals(List.of(1, 2, 3, 4), drawPositions);
    }
    
    @Test
    void testPick3Configuration() {
        GameConfiguration config = new GameConfiguration("NY", "Pick3");
        
        assertEquals(0, config.getMinNumber());
        assertEquals(9, config.getMaxNumber());
        assertEquals(3, config.getNumbersDrawn());
        assertEquals(0, config.getBonusNumbers());
        assertEquals(2, config.getDefaultRangeSize());
        assertEquals(30, config.getRecommendedMaxDraws());
        
        List<Integer> drawPositions = config.getDrawPositions();
        assertEquals(List.of(1, 2, 3), drawPositions);
    }
    
    @Test
    void testCash4LifeConfiguration() {
        GameConfiguration config = new GameConfiguration("FL", "Cash4Life");
        
        assertEquals(1, config.getMinNumber());
        assertEquals(60, config.getMaxNumber());
        assertEquals(5, config.getNumbersDrawn());
        assertEquals(1, config.getBonusNumbers());
        assertEquals(10, config.getDefaultRangeSize());
        assertEquals(75, config.getRecommendedMaxDraws());
    }
    
    @Test
    void testCashFiveConfiguration() {
        GameConfiguration config = new GameConfiguration("TX", "Cash Five");
        
        assertEquals(1, config.getMinNumber());
        assertEquals(39, config.getMaxNumber());
        assertEquals(5, config.getNumbersDrawn());
        assertEquals(0, config.getBonusNumbers());
        assertEquals(5, config.getDefaultRangeSize());
        assertEquals(60, config.getRecommendedMaxDraws());
    }
    
    @Test
    void testGenericLotteryConfiguration() {
        GameConfiguration config = new GameConfiguration("", "Unknown Game");
        
        assertEquals(1, config.getMinNumber());
        assertEquals(49, config.getMaxNumber());
        assertEquals(5, config.getNumbersDrawn());
        assertEquals(0, config.getBonusNumbers());
        assertEquals(10, config.getDefaultRangeSize());
        assertEquals(50, config.getRecommendedMaxDraws());
    }
    
    @ParameterizedTest
    @CsvSource({
        "Powerball, 5, true",
        "Powerball, 10, true", 
        "Powerball, 15, true",
        "Powerball, 20, false",
        "Pick4, 2, true",
        "Pick4, 5, true",
        "Pick4, 10, false",
        "Cash Five, 5, true",
        "Cash Five, 10, true",
        "Cash Five, 15, false"
    })
    void testValidRangeSizes(String gameName, int rangeSize, boolean expectedValid) {
        GameConfiguration config = new GameConfiguration("", gameName);
        assertEquals(expectedValid, config.isValidRangeSize(rangeSize));
    }
    
    @Test
    void testGetValidRangeSizesPowerball() {
        GameConfiguration config = new GameConfiguration("", "Powerball");
        List<Integer> validSizes = config.getValidRangeSizes();
        
        assertTrue(validSizes.contains(5));
        assertTrue(validSizes.contains(10)); 
        assertTrue(validSizes.contains(15));
        assertFalse(validSizes.contains(20)); // 20 would create ranges too large for 69 numbers
    }
    
    @Test
    void testGetValidRangeSizesPick4() {
        GameConfiguration config = new GameConfiguration("", "Pick4");
        List<Integer> validSizes = config.getValidRangeSizes();
        
        assertTrue(validSizes.contains(2));
        assertTrue(validSizes.contains(5));
        assertEquals(2, validSizes.size()); // Only 2 and 5 should be valid for 0-9 range
    }
    
    @Test
    void testGenerateValidRangeHeadersPowerball() {
        GameConfiguration config = new GameConfiguration("", "Powerball");
        List<String> ranges = config.generateValidRangeHeaders(10);
        
        assertEquals("1-10", ranges.get(0));
        assertEquals("11-20", ranges.get(1));
        assertEquals("21-30", ranges.get(2));
        assertEquals("61-69", ranges.get(ranges.size() - 1)); // Last range should end at 69
        assertEquals(7, ranges.size()); // Should have 7 ranges for Powerball with size 10
    }
    
    @Test
    void testGenerateValidRangeHeadersPick4() {
        GameConfiguration config = new GameConfiguration("", "Pick4");
        List<String> ranges = config.generateValidRangeHeaders(2);
        
        assertEquals("0-1", ranges.get(0));
        assertEquals("2-3", ranges.get(1));
        assertEquals("4-5", ranges.get(2));
        assertEquals("6-7", ranges.get(3));
        assertEquals("8-9", ranges.get(4));
        assertEquals(5, ranges.size()); // Should have 5 ranges for Pick4 with size 2
    }
    
    @Test
    void testGenerateValidRangeHeadersOddSizeRange() {
        GameConfiguration config = new GameConfiguration("", "Cash Five"); // 1-39 range
        List<String> ranges = config.generateValidRangeHeaders(5);
        
        assertEquals("1-5", ranges.get(0));
        assertEquals("6-10", ranges.get(1));
        assertEquals("31-35", ranges.get(6));
        assertEquals("36-39", ranges.get(7)); // Last range should be smaller
        assertEquals(8, ranges.size()); // Should have 8 ranges for Cash Five with size 5
    }
    
    @Test
    void testRangePreviewTextValid() {
        GameConfiguration config = new GameConfiguration("", "Pick4");
        String preview = config.getRangePreviewText(2);
        
        assertTrue(preview.startsWith("Ranges: "));
        assertTrue(preview.contains("0-1"));
        assertTrue(preview.contains("2-3"));
        assertTrue(preview.contains("8-9"));
    }
    
    @Test
    void testRangePreviewTextInvalid() {
        GameConfiguration config = new GameConfiguration("", "Pick4");
        String preview = config.getRangePreviewText(20); // Invalid range size
        
        assertTrue(preview.contains("Invalid range size"));
        assertTrue(preview.contains("Pick4"));
        assertTrue(preview.contains("0-9"));
    }
    
    @Test
    void testRangePreviewTextTruncated() {
        GameConfiguration config = new GameConfiguration("", "Powerball"); // Larger range
        String preview = config.getRangePreviewText(5);
        
        assertTrue(preview.startsWith("Ranges: "));
        assertTrue(preview.contains("..."));
        assertTrue(preview.contains("total ranges"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"powerball", "POWERBALL", "Power Ball", "power-ball"})
    void testGameNameNormalization(String gameName) {
        GameConfiguration config = new GameConfiguration("", gameName);
        
        assertEquals(1, config.getMinNumber());
        assertEquals(69, config.getMaxNumber());
        assertEquals(5, config.getNumbersDrawn());
    }
    
    @Test
    void testOptimalMaxDrawsCalculation() {
        GameConfiguration pick3Config = new GameConfiguration("", "Pick3");
        assertEquals(30, pick3Config.getOptimalMaxDraws()); // Should be capped at 30 for Pick3
        
        GameConfiguration powerballConfig = new GameConfiguration("", "Powerball");
        assertEquals(100, powerballConfig.getOptimalMaxDraws()); // Should be at least 75 for 5+ positions
    }
    
    @Test
    void testCalculateOptimalRangeSize() {
        GameConfiguration pick3Config = new GameConfiguration("", "Pick3");
        assertEquals(2, pick3Config.calculateOptimalRangeSize()); // 10 numbers -> size 2
        
        GameConfiguration cashFiveConfig = new GameConfiguration("", "Cash Five");
        assertEquals(5, cashFiveConfig.calculateOptimalRangeSize()); // 39 numbers -> size 5
        
        GameConfiguration powerballConfig = new GameConfiguration("", "Powerball");
        assertEquals(15, powerballConfig.calculateOptimalRangeSize()); // 69 numbers -> size 15
    }
    
    @Test
    void testRangeSizeValidationBoundaryConditions() {
        GameConfiguration config = new GameConfiguration("", "Pick4"); // 0-9 range, 10 total numbers
        
        // Edge case: range size equal to total numbers should be invalid
        assertFalse(config.isValidRangeSize(10));
        
        // Edge case: range size that would create only 1 range should be invalid
        assertFalse(config.isValidRangeSize(8));
        
        // Valid sizes that create multiple ranges
        assertTrue(config.isValidRangeSize(2)); // 5 ranges
        assertTrue(config.isValidRangeSize(5)); // 2 ranges
    }
    
    @Test
    void testPreventInfiniteLoop() {
        GameConfiguration config = new GameConfiguration("", "Pick3");
        List<String> ranges = config.generateValidRangeHeaders(1);
        
        // Should not exceed 50 ranges (safety limit)
        assertTrue(ranges.size() <= 50);
        assertEquals("0-0", ranges.get(0));
        assertEquals("9-9", ranges.get(9));
        assertEquals(10, ranges.size()); // Should have 10 ranges for 0-9 with size 1
    }
}