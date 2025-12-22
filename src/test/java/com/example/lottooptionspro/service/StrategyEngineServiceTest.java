package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.strategyengine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StrategyEngineServiceTest {

    private StrategyEngineService sut;

    @BeforeEach
    void setUp() {
        sut = new StrategyEngineService();
    }

    @Test
    @DisplayName("Should create request with game profile and preferences")
    void testCreateRequest_Success() {
        String gameId = "Lotto Texas";
        String lotteryState = "Texas";
        Integer poolSize = 18;
        Integer setCount = 4;
        String strategyBias = "aggressive";

        GameProfile gameProfile = new GameProfile(gameId, lotteryState);
        Preferences preferences = new Preferences(poolSize, setCount, strategyBias);
        StrategyEngineRequest request = new StrategyEngineRequest(gameProfile, preferences);

        assertNotNull(request);
        assertEquals(gameId, request.getGameProfile().getGameId());
        assertEquals(lotteryState, request.getGameProfile().getLotteryState());
        assertEquals(poolSize, request.getPreferences().getPoolSize());
        assertEquals(setCount, request.getPreferences().getSetCount());
        assertEquals(strategyBias, request.getPreferences().getStrategyBias());
    }

    @Test
    @DisplayName("Should map engine constants correctly")
    void testEngineConstants_Mapping() {
        EngineConstants constants = new EngineConstants(8.0, 17, 8);

        assertNotNull(constants);
        assertEquals(8.0, constants.getAverageSkip());
        assertEquals(17, constants.getLongShotThreshold());
        assertEquals(8, constants.getColdRuleLimit());
    }

    @Test
    @DisplayName("Should map tier1 anchors with patterns")
    void testTier1Anchors_Mapping() {
        Map<String, String> patterns = new HashMap<>();
        patterns.put("6", "Trend Reversal (Hit after 25 skips)");
        patterns.put("9", "Trend Reversal (Hit after 44 skips)");
        patterns.put("10", "Cascade (Momentum: 1.00)");

        Tier1Anchors anchors = new Tier1Anchors(
            Arrays.asList(6, 9, 10, 12, 19, 20, 21, 28, 30, 33, 43, 49),
            patterns
        );

        assertNotNull(anchors);
        assertEquals(12, anchors.getNumbers().size());
        assertTrue(anchors.getNumbers().contains(6));
        assertTrue(anchors.getNumbers().contains(49));
        assertEquals(3, anchors.getPatterns().size());
        assertTrue(anchors.getPatterns().containsKey("6"));
    }

    @Test
    @DisplayName("Should map generated set with all fields")
    void testGeneratedSet_Mapping() {
        TierBreakdown tierBreakdown = new TierBreakdown(
            Arrays.asList(6, 9, 10, 12, 19, 20, 21, 28, 30, 33, 43, 49),
            Arrays.asList(3, 5, 24, 31, 35, 36),
            Arrays.asList()
        );

        MatchStatistic perfectMatch = new MatchStatistic("6/6", 0, 200, null, 0.0);
        MatchStatistic matchMinus1 = new MatchStatistic("5/6", 3, 0, "2025-12-20", 1.5);
        TrapStatistics trapStats = new TrapStatistics(perfectMatch, matchMinus1, null, null);
        SetPerformance performance = new SetPerformance(trapStats, 1.95, 5, 0);

        GeneratedSet set = new GeneratedSet(
            1,
            Arrays.asList(3, 5, 6, 9, 10, 12, 19, 20, 21, 24, 28, 30, 31, 33, 35, 36, 43, 49),
            1.0,
            tierBreakdown,
            performance
        );

        assertNotNull(set);
        assertEquals(1, set.getSetId());
        assertEquals(18, set.getNumbers().size());
        assertEquals(1.0, set.getDiversityScore());
        assertEquals(12, set.getTierBreakdown().getTier1Anchors().size());
        assertEquals(6, set.getTierBreakdown().getTier2Rotators().size());
        assertEquals(1.95, set.getSetPerformance().getAverageCoverage());
    }

    @Test
    @DisplayName("Should map exclusion report correctly")
    void testExclusionReport_Mapping() {
        ExclusionReport exclusion = new ExclusionReport(1, "Cold Rule Violated", 10, 8, null);

        assertNotNull(exclusion);
        assertEquals(1, exclusion.getNumber());
        assertEquals("Cold Rule Violated", exclusion.getReason());
        assertEquals(10, exclusion.getCurrentSkip());
        assertEquals(8, exclusion.getLimit());
        assertNull(exclusion.getNote());
    }

    @Test
    @DisplayName("Should map historical performance correctly")
    void testHistoricalPerformance_Mapping() {
        MatchStatistic perfectMatch = new MatchStatistic("6/6", 43, 0, "2025-12-20", 21.5);
        TrapStatistics trapStats = new TrapStatistics(perfectMatch, null, null, null);
        PerformanceMetrics metrics = new PerformanceMetrics(87.5, 4.63, 6, 79.57);
        
        HistoricalPerformance performance = new HistoricalPerformance(trapStats, metrics, 200);

        assertNotNull(performance);
        assertEquals(200, performance.getDrawsAnalyzed());
        assertEquals(87.5, performance.getPerformanceMetrics().getHitRate());
        assertEquals(4.63, performance.getPerformanceMetrics().getAverageCoverage());
        assertEquals(43, performance.getTrapStatistics().getPerfectMatch().getCount());
    }

    @Test
    @DisplayName("Should map metadata correctly")
    void testMetadata_Mapping() {
        Metadata metadata = new Metadata(
            "Lotto Texas",
            "Texas",
            54,
            null,
            200,
            54,
            41,
            "aggressive",
            3
        );

        assertNotNull(metadata);
        assertEquals("Lotto Texas", metadata.getGameId());
        assertEquals("Texas", metadata.getLotteryState());
        assertEquals(54, metadata.getFieldSize());
        assertEquals(200, metadata.getHistoricalDrawsUsed());
        assertEquals(54, metadata.getTotalCandidatesEvaluated());
        assertEquals(41, metadata.getCandidatesAfterFiltering());
        assertEquals("aggressive", metadata.getStrategyBias());
        assertEquals(3, metadata.getExecutionTimeMs());
    }

    @Test
    @DisplayName("Should create complete strategy engine response")
    void testStrategyEngineResponse_Complete() {
        EngineConstants constants = new EngineConstants(8.0, 17, 8);
        Tier1Anchors anchors = new Tier1Anchors(Arrays.asList(6, 9, 10), new HashMap<>());
        GeneratedSet set = new GeneratedSet(1, Arrays.asList(1, 2, 3), 1.0, null, null);
        ExclusionReport exclusion = new ExclusionReport(1, "Cold Rule Violated", 10, 8, null);
        HistoricalPerformance performance = new HistoricalPerformance(null, null, 200);
        Metadata metadata = new Metadata("Lotto Texas", "Texas", 54, null, 200, 54, 41, "aggressive", 3);

        StrategyEngineResponse response = new StrategyEngineResponse(
            constants,
            anchors,
            Arrays.asList(set),
            Arrays.asList(exclusion),
            performance,
            metadata
        );

        assertNotNull(response);
        assertNotNull(response.getEngineConstants());
        assertNotNull(response.getTier1Anchors());
        assertEquals(1, response.getGeneratedSets().size());
        assertEquals(1, response.getExclusionReport().size());
        assertNotNull(response.getHistoricalPerformance());
        assertNotNull(response.getMetadata());
    }

    @Test
    @DisplayName("Should validate preferences values")
    void testPreferences_Validation() {
        Preferences preferences = new Preferences(18, 4, "aggressive");

        assertNotNull(preferences);
        assertTrue(preferences.getPoolSize() >= 4 && preferences.getPoolSize() <= 27);
        assertTrue(preferences.getSetCount() > 0);
        assertTrue(Arrays.asList("aggressive", "balanced", "conservative").contains(preferences.getStrategyBias()));
    }

    @Test
    @DisplayName("Should handle tier breakdown with empty tier3")
    void testTierBreakdown_EmptyTier3() {
        TierBreakdown tierBreakdown = new TierBreakdown(
            Arrays.asList(6, 9, 10),
            Arrays.asList(3, 5),
            Arrays.asList()
        );

        assertNotNull(tierBreakdown);
        assertEquals(3, tierBreakdown.getTier1Anchors().size());
        assertEquals(2, tierBreakdown.getTier2Rotators().size());
        assertTrue(tierBreakdown.getTier3Rotators().isEmpty());
    }
}
