package com.example.lottooptionspro.service;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for locating betslip template files based on state and game name.
 * Handles multiple naming conventions and provides fallback mechanisms.
 * 
 * Note: This is a utility class, not a Spring-managed bean.
 */
public class TemplateLocatorService {

    /**
     * Find a template file for the given state and game name.
     * Tries multiple naming conventions and returns the first match found.
     *
     * @param stateName The state/jurisdiction name (e.g., "TEXAS", "Texas")
     * @param gameName The game name (e.g., "Cash Five", "Lotto Texas")
     * @return Optional containing the File if found, empty otherwise
     */
    public Optional<File> findTemplateFile(String stateName, String gameName) {
        if (stateName == null || stateName.trim().isEmpty() || 
            gameName == null || gameName.trim().isEmpty()) {
            return Optional.empty();
        }

        String[] possiblePaths = getPossibleResourcePaths(stateName, gameName);
        
        for (String path : possiblePaths) {
            try {
                URL resource = this.getClass().getResource(path);
                if (resource != null) {
                    File file = new File(resource.toURI());
                    if (file.exists() && file.isFile()) {
                        System.out.println("Found template at: " + path);
                        return Optional.of(file);
                    }
                }
            } catch (URISyntaxException e) {
                System.err.println("Error converting resource URL to file: " + path);
            }
        }
        
        System.out.println("No template found for State: '" + stateName + "', Game: '" + gameName + "'");
        return Optional.empty();
    }

    /**
     * Find all matching template files for the given state and game name.
     * Useful when multiple templates exist with different naming conventions.
     *
     * @param stateName The state/jurisdiction name
     * @param gameName The game name
     * @return List of matching Files (may be empty)
     */
    public List<File> findAllMatchingTemplates(String stateName, String gameName) {
        List<File> matches = new ArrayList<>();
        
        if (stateName == null || stateName.trim().isEmpty() || 
            gameName == null || gameName.trim().isEmpty()) {
            return matches;
        }

        String[] possiblePaths = getPossibleResourcePaths(stateName, gameName);
        
        for (String path : possiblePaths) {
            try {
                URL resource = this.getClass().getResource(path);
                if (resource != null) {
                    File file = new File(resource.toURI());
                    if (file.exists() && file.isFile()) {
                        matches.add(file);
                    }
                }
            } catch (URISyntaxException e) {
                System.err.println("Error converting resource URL to file: " + path);
            }
        }
        
        return matches;
    }

    /**
     * Get all possible resource paths for a given state and game name.
     * Tries multiple naming conventions commonly used in the application.
     *
     * @param stateName The state/jurisdiction name
     * @param gameName The game name
     * @return Array of possible resource paths to check
     */
    public String[] getPossibleResourcePaths(String stateName, String gameName) {
        // Normalize state name - handle both "TEXAS" and "Texas"
        String stateCapitalized = capitalize(stateName);
        String stateLower = stateName.toLowerCase();
        
        // Normalize game name
        String gameNoSpaces = gameName.replaceAll("\\s+", "");
        String gameLower = gameName.toLowerCase().replaceAll("\\s+", "");
        
        return new String[] {
            // Convention 1: /images/StateName/GameName.json (e.g., /images/Texas/CashFive.json)
            "/images/" + stateCapitalized + "/" + gameNoSpaces + ".json",
            
            // Convention 2: /images/StateName/statename-gamename.json (e.g., /images/Texas/texas-lottotexas.json)
            "/images/" + stateCapitalized + "/" + stateLower + "-" + gameLower + ".json",
            
            // Convention 3: /images/StateName/gamename.json (e.g., /images/Texas/cashfive.json)
            "/images/" + stateCapitalized + "/" + gameLower + ".json",
            
            // Convention 4: /images/STATENAME/statename-gamename.json (all caps directory)
            "/images/" + stateName.toUpperCase() + "/" + stateLower + "-" + gameLower + ".json",
            
            // Convention 5: /images/statename/statename-gamename.json (all lowercase)
            "/images/" + stateLower + "/" + stateLower + "-" + gameLower + ".json"
        };
    }

    /**
     * Get the suggested file name for saving a template.
     *
     * @param stateName The state/jurisdiction name
     * @param gameName The game name
     * @return Suggested filename following the standard convention
     */
    public String getSuggestedFileName(String stateName, String gameName) {
        if (stateName == null || gameName == null) {
            return "template.json";
        }
        
        String stateLower = stateName.toLowerCase().replaceAll("\\s+", "");
        String gameLower = gameName.toLowerCase().replaceAll("\\s+", "");
        
        return stateLower + "-" + gameLower + ".json";
    }

    /**
     * Get the suggested directory path for templates of a given state.
     *
     * @param stateName The state/jurisdiction name
     * @return Path to the state's template directory
     */
    public String getSuggestedDirectoryPath(String stateName) {
        if (stateName == null || stateName.trim().isEmpty()) {
            return "src/main/resources/images";
        }
        
        String stateCapitalized = capitalize(stateName);
        return "src/main/resources/images/" + stateCapitalized;
    }

    /**
     * Check if a template exists for the given state and game.
     *
     * @param stateName The state/jurisdiction name
     * @param gameName The game name
     * @return true if at least one template file exists, false otherwise
     */
    public boolean hasTemplate(String stateName, String gameName) {
        return findTemplateFile(stateName, gameName).isPresent();
    }

    /**
     * Capitalize the first letter of each word in a string.
     *
     * @param input The input string
     * @return Capitalized string
     */
    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
}
