package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.GuaranteeLevel;
import com.example.lottooptionspro.model.wheel.WheelTable;
import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class WheelGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WheelGenerationService.class);
    private final CoveringDesignWheelService coveringDesignService;
    private final WheelVerificationService verificationService;
    
    public WheelGenerationService(CoveringDesignWheelService coveringDesignService, 
                                 WheelVerificationService verificationService) {
        this.coveringDesignService = coveringDesignService;
        this.verificationService = verificationService;
    }
    
    public CompletableFuture<WheelTable> generateWheelWithProgress(
            int poolSize,
            int pickSize,
            GuaranteeLevel guaranteeLevel,
            WheelProgressCallback progressCallback,
            AtomicBoolean cancelled) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                WheelParameters params = new WheelParameters(poolSize, pickSize, guaranteeLevel);
                params.validate();

                progressCallback.update("Building optimal wheel...", 0.05, 0, 0, 0);

                List<int[]> wheel = coveringDesignService.buildAndSaveWheel(params, progressCallback, cancelled);
                
                if (cancelled.get()) {
                    throw new RuntimeException("Generation cancelled");
                }
                
                progressCallback.update("Verifying wheel coverage...", 0.95, 0, 0, wheel.size());
                
                WheelVerificationService.VerificationResult verification = 
                    verificationService.verify(wheel, params);
                
                progressCallback.update("Complete!", 1.0, verification.getPassedDraws(), 
                                      verification.getTotalDraws(), wheel.size());
                
                WheelTable table = new WheelTable();
                table.setPoolSize(poolSize);
                table.setPickSize(pickSize);
                table.setGuaranteeLevel(guaranteeLevel);
                table.setTotalLines(wheel.size());
                table.setSource(String.format("Covering Design C(%d,%d,%d,%d)", 
                    params.getV(), params.getK(), params.getT(), params.getM()));
                table.setVerified(verification.isVerified());
                table.setPatterns(wheel);
                
                logger.info("Wheel generated: {} lines, verified: {}, coverage: {}%",
                           wheel.size(), verification.isVerified(), String.format("%.2f", verification.getCoveragePercentage()));
                
                return table;
                
            } catch (Exception e) {
                logger.error("Wheel generation failed", e);
                throw new RuntimeException("Generation failed: " + e.getMessage(), e);
            }
        });
    }
}
