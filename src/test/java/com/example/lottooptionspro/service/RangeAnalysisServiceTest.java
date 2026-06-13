package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.range.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RangeAnalysisServiceTest {

    private RangeAnalysisService rangeAnalysisService;

    @BeforeEach
    void setUp() {
        rangeAnalysisService = new RangeAnalysisService(WebClient.builder().build());
    }

    @Test
    void testCreateDefaultRequest() {
        String state = "FL";
        String game = "Cash4Life";

        RangeAnalysisRequest request = rangeAnalysisService.createDefaultRequest(state, game);

        assertNotNull(request);
        assertEquals("FL", request.getLotteryState());
        assertEquals("Cash4Life", request.getLotteryGame());
        assertEquals(AnalysisType.ACTUAL, request.getAnalysisType());
        assertEquals(10, request.getRangeSize()); // Cash4Life uses default 10
        assertEquals(75, request.getMaxDraws()); // Cash4Life recommended draws
        assertTrue(request.isIncludePerformanceMetrics());
        assertNotNull(request.getDrawPositions());
        assertEquals(5, request.getDrawPositions().size()); // Cash4Life has 5 positions
    }
    
    @Test
    void testGameSpecificConfigurations() {
        // Test Pick4 game
        RangeAnalysisRequest pick4Request = rangeAnalysisService.createDefaultRequest("TX", "Pick4");
        assertEquals(2, pick4Request.getRangeSize());
        assertEquals(40, pick4Request.getMaxDraws());
        assertEquals(4, pick4Request.getDrawPositions().size());
        
        // Test Powerball game
        RangeAnalysisRequest powerballRequest = rangeAnalysisService.createDefaultRequest("CA", "Powerball");
        assertEquals(10, powerballRequest.getRangeSize());
        assertEquals(100, powerballRequest.getMaxDraws());
        assertEquals(5, powerballRequest.getDrawPositions().size());
        
        // Test Pick3 game
        RangeAnalysisRequest pick3Request = rangeAnalysisService.createDefaultRequest("NY", "Pick3");
        assertEquals(2, pick3Request.getRangeSize());
        assertEquals(30, pick3Request.getMaxDraws());
        assertEquals(3, pick3Request.getDrawPositions().size());
    }
    
    @Test
    void testGameConfiguration() {
        GameConfiguration cash4LifeConfig = rangeAnalysisService.getGameConfiguration("FL", "Cash4Life");
        assertEquals("FL", cash4LifeConfig.getState());
        assertEquals("Cash4Life", cash4LifeConfig.getGame());
        assertEquals(1, cash4LifeConfig.getMinNumber());
        assertEquals(60, cash4LifeConfig.getMaxNumber());
        assertEquals(5, cash4LifeConfig.getNumbersDrawn());
        assertEquals(1, cash4LifeConfig.getBonusNumbers());
        assertTrue(cash4LifeConfig.hasBonus());
        assertEquals("1-60", cash4LifeConfig.getNumberRangeString());
        
        GameConfiguration pick4Config = rangeAnalysisService.getGameConfiguration("TX", "Pick4");
        assertEquals(0, pick4Config.getMinNumber());
        assertEquals(9, pick4Config.getMaxNumber());
        assertEquals(4, pick4Config.getNumbersDrawn());
        assertEquals(0, pick4Config.getBonusNumbers());
        assertFalse(pick4Config.hasBonus());
    }
    
    @Test
    void testSupportedRangeSizesForGame() {
        List<Integer> cash4LifeSizes = rangeAnalysisService.getSupportedRangeSizesForGame("FL", "Cash4Life");
        assertTrue(cash4LifeSizes.contains(5));
        assertTrue(cash4LifeSizes.contains(10));
        assertTrue(cash4LifeSizes.contains(12));
        
        List<Integer> pick4Sizes = rangeAnalysisService.getSupportedRangeSizesForGame("TX", "Pick4");
        assertTrue(pick4Sizes.contains(2));
        assertTrue(pick4Sizes.contains(5));
        assertEquals(2, pick4Sizes.size()); // Should only have 2 options for Pick4
    }

    @Test
    void testGetSupportedAnalysisTypes() {
        List<AnalysisType> types = rangeAnalysisService.getSupportedAnalysisTypes();

        assertNotNull(types);
        assertTrue(types.contains(AnalysisType.ACTUAL));
        assertTrue(types.contains(AnalysisType.DELTA));
        assertTrue(types.contains(AnalysisType.DELTA_SORTED));
    }

    @Test
    void testGetSupportedRangeSizes() {
        List<Integer> rangeSizes = rangeAnalysisService.getSupportedRangeSizes();

        assertNotNull(rangeSizes);
        assertTrue(rangeSizes.contains(10));
        assertTrue(rangeSizes.contains(5));
        assertTrue(rangeSizes.contains(15));
        assertTrue(rangeSizes.contains(20));
    }

    @Test
    void testGetDefaultPositionsForGame() {
        List<Integer> powerballPositions = rangeAnalysisService.getDefaultPositionsForGame("powerball");
        List<Integer> pick4Positions = rangeAnalysisService.getDefaultPositionsForGame("pick4");
        List<Integer> pick3Positions = rangeAnalysisService.getDefaultPositionsForGame("pick3");
        List<Integer> defaultPositions = rangeAnalysisService.getDefaultPositionsForGame("unknown");

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), powerballPositions);
        assertEquals(Arrays.asList(1, 2, 3, 4), pick4Positions);
        assertEquals(Arrays.asList(1, 2, 3), pick3Positions);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), defaultPositions);
    }

    @Test
    void testAnalysisTypeEnum() {
        assertEquals("Standard lottery analysis with position-specific patterns", 
                    AnalysisType.ACTUAL.getDescription());
        assertEquals("ACTUAL", AnalysisType.ACTUAL.getDisplayName());
        
        assertEquals("Spacing pattern analysis for consistent gap patterns", 
                    AnalysisType.DELTA.getDescription());
        assertEquals("DELTA", AnalysisType.DELTA.getDisplayName());
        
        assertEquals("Normalized pattern recognition for order-independent analysis", 
                    AnalysisType.DELTA_SORTED.getDescription());
        assertEquals("DELTA SORTED", AnalysisType.DELTA_SORTED.getDisplayName());
    }

    @Test
    void testPerformanceMetricsUtilities() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setStatus("HOT");
        metrics.setCurrentStreak("HOT_3");

        assertTrue(metrics.isHot());
        assertFalse(metrics.isCold());
        assertFalse(metrics.isNormal());
        assertEquals("HOT (HOT 3)", metrics.getStatusDisplayName());

        metrics.setStatus("COLD");
        assertFalse(metrics.isHot());
        assertTrue(metrics.isCold());
        assertFalse(metrics.isNormal());

        metrics.setStatus("NORMAL");
        assertFalse(metrics.isHot());
        assertFalse(metrics.isCold());
        assertTrue(metrics.isNormal());
    }

    @Test
    void testPositionAnalysisUtilities() {
        PositionAnalysis analysis = new PositionAnalysis();
        analysis.setPosition(1);
        analysis.setCurrentPositionStreak("1-10_RECENT");

        assertEquals("Position 1", analysis.getPositionDisplayName());
        assertEquals("1-10 RECENT", analysis.getStreakDisplayName());

        analysis.setCurrentPositionStreak(null);
        assertEquals("No streak", analysis.getStreakDisplayName());
    }

    @Test
    void testRangeAnalysisResponseUtilities() {
        RangeAnalysisResponse response = new RangeAnalysisResponse();
        
        assertFalse(response.hasData());
        assertFalse(response.hasPerformanceMetrics());
        assertFalse(response.hasPositionAnalysis());

        response.setDrawResults(Arrays.asList(new DrawResult()));
        assertTrue(response.hasData());

        // Test null safety
        assertNull(response.getPerformanceMetricsForRange("1-10"));
        assertNull(response.getPositionAnalysisForPosition("position1"));
    }
    
    @Test
    void testGenerateRangeHeadersForGame() {
        // Test Powerball range headers
        List<String> powerballRanges = rangeAnalysisService.generateRangeHeadersForGame("CA", "Powerball", 10);
        assertNotNull(powerballRanges);
        assertEquals("1-10", powerballRanges.get(0));
        assertEquals("11-20", powerballRanges.get(1));
        assertEquals("61-69", powerballRanges.get(powerballRanges.size() - 1));
        assertEquals(7, powerballRanges.size());
        
        // Test Pick4 range headers
        List<String> pick4Ranges = rangeAnalysisService.generateRangeHeadersForGame("TX", "Pick4", 2);
        assertNotNull(pick4Ranges);
        assertEquals("0-1", pick4Ranges.get(0));
        assertEquals("2-3", pick4Ranges.get(1));
        assertEquals("8-9", pick4Ranges.get(pick4Ranges.size() - 1));
        assertEquals(5, pick4Ranges.size());
        
        // Test Pick3 range headers
        List<String> pick3Ranges = rangeAnalysisService.generateRangeHeadersForGame("NY", "Pick3", 2);
        assertNotNull(pick3Ranges);
        assertEquals(5, pick3Ranges.size()); // Same as Pick4 since both use 0-9 range
    }
    
    @Test
    void testIsValidRangeSizeForGame() {
        // Test Powerball valid range sizes
        assertTrue(rangeAnalysisService.isValidRangeSizeForGame("CA", "Powerball", 5));
        assertTrue(rangeAnalysisService.isValidRangeSizeForGame("CA", "Powerball", 10));
        assertTrue(rangeAnalysisService.isValidRangeSizeForGame("CA", "Powerball", 15));
        assertFalse(rangeAnalysisService.isValidRangeSizeForGame("CA", "Powerball", 20)); // Too large
        
        // Test Pick4 valid range sizes
        assertTrue(rangeAnalysisService.isValidRangeSizeForGame("TX", "Pick4", 2));
        assertTrue(rangeAnalysisService.isValidRangeSizeForGame("TX", "Pick4", 5));
        assertFalse(rangeAnalysisService.isValidRangeSizeForGame("TX", "Pick4", 10)); // Too large for 0-9 range
        
        // Test Cash Five valid range sizes
        assertTrue(rangeAnalysisService.isValidRangeSizeForGame("TX", "Cash Five", 5));
        assertTrue(rangeAnalysisService.isValidRangeSizeForGame("TX", "Cash Five", 10));
        assertFalse(rangeAnalysisService.isValidRangeSizeForGame("TX", "Cash Five", 15)); // Too large for 1-39 range
    }
    
    @Test
    void testGetRangePreviewForGame() {
        // Test Pick4 range preview
        String pick4Preview = rangeAnalysisService.getRangePreviewForGame("TX", "Pick4", 2);
        assertNotNull(pick4Preview);
        assertTrue(pick4Preview.startsWith("Ranges: "));
        assertTrue(pick4Preview.contains("0-1"));
        assertTrue(pick4Preview.contains("8-9"));
        
        // Test invalid range size
        String invalidPreview = rangeAnalysisService.getRangePreviewForGame("TX", "Pick4", 20);
        assertNotNull(invalidPreview);
        assertTrue(invalidPreview.contains("Invalid range size"));
        assertTrue(invalidPreview.contains("Pick4"));
        
        // Test Powerball range preview (should be truncated due to many ranges)
        String powerballPreview = rangeAnalysisService.getRangePreviewForGame("CA", "Powerball", 5);
        assertNotNull(powerballPreview);
        assertTrue(powerballPreview.startsWith("Ranges: "));
        if (powerballPreview.contains("...")) {
            assertTrue(powerballPreview.contains("total ranges"));
        }
    }
    
    @Test
    void testGetSupportedRangeSizesForGameVsGeneric() {
        // Test that game-specific method returns different results than generic method
        List<Integer> genericSizes = rangeAnalysisService.getSupportedRangeSizes();
        List<Integer> pick4Sizes = rangeAnalysisService.getSupportedRangeSizesForGame("TX", "Pick4");
        List<Integer> powerballSizes = rangeAnalysisService.getSupportedRangeSizesForGame("CA", "Powerball");
        
        // Pick4 should have fewer valid sizes than generic
        assertTrue(pick4Sizes.size() < genericSizes.size());
        assertFalse(pick4Sizes.contains(15)); // 15 is in generic but not valid for Pick4
        
        // Powerball should also have fewer valid sizes than generic due to upper bound limits
        assertTrue(powerballSizes.contains(5));
        assertTrue(powerballSizes.contains(10));
        assertTrue(powerballSizes.contains(15));
    }
    
    @Test
    void testRangeValidationConsistency() {
        String[] games = {"Powerball", "Mega Millions", "Pick4", "Pick3", "Cash Five", "Cash4Life"};
        String[] states = {"CA", "FL", "TX", "NY"};
        
        for (String game : games) {
            for (String state : states) {
                GameConfiguration config = rangeAnalysisService.getGameConfiguration(state, game);
                List<Integer> validSizes = rangeAnalysisService.getSupportedRangeSizesForGame(state, game);
                
                // Ensure all returned valid sizes actually pass the validation check
                for (Integer size : validSizes) {
                    assertTrue(rangeAnalysisService.isValidRangeSizeForGame(state, game, size),
                        String.format("Range size %d should be valid for %s %s but validation failed", size, state, game));
                    
                    // Ensure we can generate headers for all valid sizes
                    List<String> headers = rangeAnalysisService.generateRangeHeadersForGame(state, game, size);
                    assertNotNull(headers);
                    assertFalse(headers.isEmpty());
                }
            }
        }
    }
    
    @Test
    void testBoundaryConditions() {
        // Test edge case: very small range (Pick3/Pick4)
        List<Integer> pick3ValidSizes = rangeAnalysisService.getSupportedRangeSizesForGame("NY", "Pick3");
        for (Integer size : pick3ValidSizes) {
            List<String> ranges = rangeAnalysisService.generateRangeHeadersForGame("NY", "Pick3", size);
            assertTrue(ranges.size() >= 2, "Should generate at least 2 ranges to be meaningful");
            assertTrue(ranges.size() <= 10, "Should not generate more than 10 ranges for 10 total numbers");
        }
        
        // Test edge case: larger range (Powerball/Mega Millions)
        List<Integer> powerballValidSizes = rangeAnalysisService.getSupportedRangeSizesForGame("CA", "Powerball");
        for (Integer size : powerballValidSizes) {
            List<String> ranges = rangeAnalysisService.generateRangeHeadersForGame("CA", "Powerball", size);
            assertTrue(ranges.size() >= 2, "Should generate at least 2 ranges to be meaningful");
            
            // Verify last range ends at correct number
            String lastRange = ranges.get(ranges.size() - 1);
            assertTrue(lastRange.endsWith("69"), "Last range should end at 69 for Powerball");
        }
    }
}