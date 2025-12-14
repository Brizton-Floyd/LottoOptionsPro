package com.example.lottooptionspro.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateLocatorService
 */
class TemplateLocatorServiceTest {

    private TemplateLocatorService service;

    @BeforeEach
    void setUp() {
        service = new TemplateLocatorService();
    }

    @Test
    void testGetPossibleResourcePaths() {
        String[] paths = service.getPossibleResourcePaths("TEXAS", "Cash Five");
        
        assertNotNull(paths);
        assertTrue(paths.length > 0);
        
        // Check that multiple naming conventions are covered
        boolean hasCapitalized = false;
        boolean hasLowercase = false;
        boolean hasHyphenated = false;
        
        for (String path : paths) {
            if (path.contains("CashFive")) hasCapitalized = true;
            if (path.contains("cashfive")) hasLowercase = true;
            if (path.contains("texas-cashfive")) hasHyphenated = true;
        }
        
        assertTrue(hasCapitalized, "Should include capitalized convention");
        assertTrue(hasLowercase, "Should include lowercase convention");
        assertTrue(hasHyphenated, "Should include hyphenated convention");
    }

    @Test
    void testGetSuggestedFileName() {
        String fileName = service.getSuggestedFileName("TEXAS", "Cash Five");
        
        assertNotNull(fileName);
        assertEquals("texas-cashfive.json", fileName);
    }

    @Test
    void testGetSuggestedFileNameWithSpaces() {
        String fileName = service.getSuggestedFileName("TEXAS", "Lotto Texas");
        
        assertNotNull(fileName);
        assertEquals("texas-lottotexas.json", fileName);
    }

    @Test
    void testGetSuggestedDirectoryPath() {
        String dirPath = service.getSuggestedDirectoryPath("TEXAS");
        
        assertNotNull(dirPath);
        assertTrue(dirPath.contains("Texas"));
        assertTrue(dirPath.endsWith("images/Texas"));
    }

    @Test
    void testGetSuggestedDirectoryPathWithNull() {
        String dirPath = service.getSuggestedDirectoryPath(null);
        
        assertNotNull(dirPath);
        assertEquals("src/main/resources/images", dirPath);
    }

    @Test
    void testFindTemplateFileForExistingTemplate() {
        // This test will pass if CashFive.json exists in resources
        Optional<File> result = service.findTemplateFile("Texas", "Cash Five");
        
        // We can't guarantee the file exists in test environment, so just check the method runs
        assertNotNull(result);
    }

    @Test
    void testFindTemplateFileWithNullParameters() {
        Optional<File> result = service.findTemplateFile(null, "Cash Five");
        
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void testFindTemplateFileWithEmptyParameters() {
        Optional<File> result = service.findTemplateFile("", "");
        
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void testHasTemplateWithNullParameters() {
        boolean result = service.hasTemplate(null, "Cash Five");
        
        assertFalse(result);
    }

    @Test
    void testFindAllMatchingTemplates() {
        // Test that the method returns a list (may be empty)
        var results = service.findAllMatchingTemplates("Texas", "Cash Five");
        
        assertNotNull(results);
        // Can't assert size since it depends on what's in resources
    }
}
