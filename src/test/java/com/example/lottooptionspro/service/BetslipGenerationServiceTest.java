package com.example.lottooptionspro.service;

import com.example.lottooptionspro.models.*;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.awt.image.BufferedImage;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BetslipGenerationService following AAA (Arrange-Act-Assert) pattern.
 * Tests focus on bonus number marking functionality and betslip generation.
 */
class BetslipGenerationServiceTest {

    private BetslipGenerationService sut;

    @BeforeAll
    static void initToolkit() {
        // Initialize JavaFX toolkit for headless testing
        new JFXPanel();
    }

    @BeforeEach
    void setUp() {
        sut = new BetslipGenerationService();
    }

    @Test
    @DisplayName("Should generate PDF with bonus markings for Powerball game")
    void testGeneratePdf_WithBonusNumbers_Success() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Powerball";
        List<int[]> numberSets = Arrays.asList(
            new int[]{5, 12, 23, 45, 67},
            new int[]{3, 15, 28, 34, 56}
        );

        // Act
        Mono<BetslipGenerationService.PdfGenerationResult> result = 
            sut.generatePdf(numberSets, stateName, gameName);

        // Assert
        StepVerifier.create(result)
            .assertNext(pdfResult -> {
                assertNotNull(pdfResult, "PDF result should not be null");
                assertNotNull(pdfResult.images, "Images list should not be null");
                assertNotNull(pdfResult.template, "Template should not be null");
                assertFalse(pdfResult.images.isEmpty(), "Should generate at least one image");
                
                // Verify template has bonus numbers
                assertTrue(pdfResult.template.getPlayPanels().stream()
                    .anyMatch(panel -> panel.getBonusNumbers() != null && !panel.getBonusNumbers().isEmpty()),
                    "Template should have panels with bonus numbers");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should generate PDF without errors for non-bonus game")
    void testGeneratePdf_WithoutBonusNumbers_Success() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Cash Five";
        List<int[]> numberSets = Arrays.asList(
            new int[]{5, 12, 23, 34, 39},
            new int[]{3, 15, 28, 31, 37}
        );

        // Act
        Mono<BetslipGenerationService.PdfGenerationResult> result = 
            sut.generatePdf(numberSets, stateName, gameName);

        // Assert
        StepVerifier.create(result)
            .assertNext(pdfResult -> {
                assertNotNull(pdfResult, "PDF result should not be null");
                assertNotNull(pdfResult.images, "Images list should not be null");
                assertFalse(pdfResult.images.isEmpty(), "Should generate at least one image");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should generate scaled PDF with bonus markings")
    void testGenerateScaledPdf_WithBonusNumbers_Success() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Powerball";
        List<int[]> numberSets = Arrays.asList(
            new int[]{5, 12, 23, 45, 67}
        );
        int targetWidth = 800;
        int targetHeight = 1200;

        // Act
        Mono<BetslipGenerationService.ScaledPdfGenerationResult> result = 
            sut.generateScaledPdf(numberSets, stateName, gameName, targetWidth, targetHeight);

        // Assert
        StepVerifier.create(result)
            .assertNext(scaledResult -> {
                assertNotNull(scaledResult, "Scaled PDF result should not be null");
                assertNotNull(scaledResult.images, "Images list should not be null");
                assertFalse(scaledResult.images.isEmpty(), "Should generate at least one image");
                assertEquals(targetWidth, scaledResult.targetWidth, "Target width should match");
                assertEquals(targetHeight, scaledResult.targetHeight, "Target height should match");
                assertTrue(scaledResult.scaleFactorX > 0, "Scale factor X should be positive");
                assertTrue(scaledResult.scaleFactorY > 0, "Scale factor Y should be positive");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty number sets gracefully")
    void testGeneratePdf_EmptyNumberSets_Success() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Powerball";
        List<int[]> numberSets = Collections.emptyList();

        // Act
        Mono<BetslipGenerationService.PdfGenerationResult> result = 
            sut.generatePdf(numberSets, stateName, gameName);

        // Assert
        StepVerifier.create(result)
            .assertNext(pdfResult -> {
                assertNotNull(pdfResult, "PDF result should not be null");
                assertNotNull(pdfResult.images, "Images list should not be null");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return error for non-existent game template")
    void testGeneratePdf_NonExistentTemplate_Error() {
        // Arrange
        String stateName = "InvalidState";
        String gameName = "InvalidGame";
        List<int[]> numberSets = Arrays.asList(new int[]{1, 2, 3, 4, 5});

        // Act
        Mono<BetslipGenerationService.PdfGenerationResult> result = 
            sut.generatePdf(numberSets, stateName, gameName);

        // Assert
        StepVerifier.create(result)
            .expectError()
            .verify();
    }

    @Test
    @DisplayName("Should correctly identify template availability for existing game")
    void testHasTemplateForGame_ExistingGame_ReturnsTrue() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Powerball";

        // Act
        boolean result = sut.hasTemplateForGame(stateName, gameName);

        // Assert
        assertTrue(result, "Should return true for existing Powerball template");
    }

    @Test
    @DisplayName("Should correctly identify template unavailability for non-existent game")
    void testHasTemplateForGame_NonExistentGame_ReturnsFalse() {
        // Arrange
        String stateName = "InvalidState";
        String gameName = "InvalidGame";

        // Act
        boolean result = sut.hasTemplateForGame(stateName, gameName);

        // Assert
        assertFalse(result, "Should return false for non-existent game template");
    }

    @Test
    @DisplayName("Should handle null state name gracefully")
    void testHasTemplateForGame_NullStateName_ReturnsFalse() {
        // Arrange
        String stateName = null;
        String gameName = "Powerball";

        // Act
        boolean result = sut.hasTemplateForGame(stateName, gameName);

        // Assert
        assertFalse(result, "Should return false for null state name");
    }

    @Test
    @DisplayName("Should handle null game name gracefully")
    void testHasTemplateForGame_NullGameName_ReturnsFalse() {
        // Arrange
        String stateName = "Texas";
        String gameName = null;

        // Act
        boolean result = sut.hasTemplateForGame(stateName, gameName);

        // Assert
        assertFalse(result, "Should return false for null game name");
    }

    @Test
    @DisplayName("Should generate multiple betslips with consistent bonus marking")
    void testGeneratePdf_MultipleBetslips_ConsistentBonusMarking() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Powerball";
        List<int[]> numberSets = Arrays.asList(
            new int[]{5, 12, 23, 45, 67},
            new int[]{3, 15, 28, 34, 56},
            new int[]{8, 19, 31, 42, 58}
        );

        // Act
        Mono<BetslipGenerationService.PdfGenerationResult> result = 
            sut.generatePdf(numberSets, stateName, gameName);

        // Assert
        StepVerifier.create(result)
            .assertNext(pdfResult -> {
                assertNotNull(pdfResult, "PDF result should not be null");
                assertFalse(pdfResult.images.isEmpty(), "Should generate images");
                
                // Verify each image is properly generated
                for (BufferedImage image : pdfResult.images) {
                    assertNotNull(image, "Each image should not be null");
                    assertTrue(image.getWidth() > 0, "Image width should be positive");
                    assertTrue(image.getHeight() > 0, "Image height should be positive");
                }
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle large number sets efficiently")
    void testGeneratePdf_LargeNumberSets_Success() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Powerball";
        List<int[]> numberSets = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            numberSets.add(new int[]{1 + i, 10 + i, 20 + i, 30 + i, 40 + i});
        }

        // Act
        Mono<BetslipGenerationService.PdfGenerationResult> result = 
            sut.generatePdf(numberSets, stateName, gameName);

        // Assert
        StepVerifier.create(result)
            .assertNext(pdfResult -> {
                assertNotNull(pdfResult, "PDF result should not be null");
                assertFalse(pdfResult.images.isEmpty(), "Should generate images for large dataset");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should apply correct scale factors in scaled generation")
    void testGenerateScaledPdf_ScaleFactors_CorrectlyApplied() {
        // Arrange
        String stateName = "Texas";
        String gameName = "Cash Five";
        List<int[]> numberSets = Arrays.asList(new int[]{5, 12, 23, 34, 39});
        int targetWidth = 600;
        int targetHeight = 900;

        // Act
        Mono<BetslipGenerationService.ScaledPdfGenerationResult> result = 
            sut.generateScaledPdf(numberSets, stateName, gameName, targetWidth, targetHeight);

        // Assert
        StepVerifier.create(result)
            .assertNext(scaledResult -> {
                assertNotNull(scaledResult, "Scaled result should not be null");
                
                // Verify images are scaled to target dimensions
                for (BufferedImage image : scaledResult.images) {
                    assertEquals(targetWidth, image.getWidth(), 
                        "Image width should match target width");
                    assertEquals(targetHeight, image.getHeight(), 
                        "Image height should match target height");
                }
            })
            .verifyComplete();
    }
}
