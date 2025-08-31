package com.example.lottooptionspro.controller;

/**
 * Utility class for tier-related calculations in the Smart Number Generator
 */
public final class TierCalculator {
    
    private TierCalculator() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Calculate optimal number of tickets for a given tier
     * @param matchCount Number of matches required
     * @param drawSize Total numbers drawn
     * @return Optimal ticket count for the tier
     */
    public static int getOptimalTicketsForTier(int matchCount, int drawSize) {
        if (matchCount == drawSize) return 20;      // Jackpot
        if (matchCount == drawSize - 1) return 30; // Near-jackpot
        if (matchCount == drawSize - 2) return 50; // Secondary
        return 75; // Lower tiers
    }
    
    /**
     * Calculate cost per ticket based on tier data
     * @param tierData Tier identifier string (e.g., "6-of-6")
     * @param jackpotCost Cost for jackpot tier
     * @param nearJackpotCost Cost for near-jackpot tier
     * @param secondaryCost Cost for secondary tier
     * @param defaultCost Cost for lower tiers
     * @param fallbackCost Fallback cost for invalid data
     * @return Cost per ticket
     */
    public static double getCostPerTicket(String tierData, double jackpotCost, double nearJackpotCost, 
                                        double secondaryCost, double defaultCost, double fallbackCost) {
        if (tierData != null && tierData.contains("-of-")) {
            try {
                String[] parts = tierData.split("-of-");
                int matchCount = Integer.parseInt(parts[0]);
                int totalCount = Integer.parseInt(parts[1]);
                
                if (matchCount == totalCount) return jackpotCost;
                if (matchCount == totalCount - 1) return nearJackpotCost;
                if (matchCount == totalCount - 2) return secondaryCost;
                return defaultCost;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Invalid tier data format: " + tierData);
            }
        }
        return fallbackCost;
    }
    
    /**
     * Parse tier data and extract match count and total count
     * @param tierData Tier identifier string
     * @return int array [matchCount, totalCount] or null if invalid
     */
    public static int[] parseTierData(String tierData) {
        if (tierData != null && tierData.contains("-of-")) {
            try {
                String[] parts = tierData.split("-of-");
                return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Invalid tier data format: " + tierData);
            }
        }
        return null;
    }
    
    /**
     * Generate display text for a tier
     * @param matchCount Number of matches
     * @param drawSize Total draw size
     * @param description Tier description
     * @param optimalTickets Optimal ticket count
     * @return Formatted tier display string
     */
    public static String formatTierDisplay(int matchCount, int drawSize, String description, int optimalTickets) {
        String cleanDescription = description.replace("Match " + matchCount + " numbers", "").trim();
        return String.format("%d-of-%d %s (%d tickets/batch)", 
                           matchCount, drawSize, cleanDescription, optimalTickets);
    }
}