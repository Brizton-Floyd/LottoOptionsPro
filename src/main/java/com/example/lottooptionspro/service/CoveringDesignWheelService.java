package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.WheelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CoveringDesignWheelService {

    private static final Logger logger = LoggerFactory.getLogger(CoveringDesignWheelService.class);
    private final PatternBasedWheelGenerator patternGenerator;
    private final OptimizedWheelBuilder optimizedBuilder;
    private final WheelPersistenceService persistenceService;

    public CoveringDesignWheelService(PatternBasedWheelGenerator patternGenerator,
                                     OptimizedWheelBuilder optimizedBuilder,
                                     WheelPersistenceService persistenceService) {
        this.patternGenerator = patternGenerator;
        this.optimizedBuilder = optimizedBuilder;
        this.persistenceService = persistenceService;
    }

    public List<int[]> constructWheel(WheelParameters params, WheelProgressCallback callback, AtomicBoolean cancelled) {
        params.validate();

        logger.info("Loading pre-computed wheel for {}", params.getNotation());

        List<int[]> templateWheel = patternGenerator.generateFromTemplate(params);
        if (templateWheel != null) {
            logger.info("Using pre-computed wheel: {} lines", templateWheel.size());
            callback.update("Loaded pre-computed wheel", 1.0, 0, 0, templateWheel.size());
            return templateWheel;
        }

        logger.warn("No pre-computed wheel found for {}. Use Wheel Builder to generate this wheel.", params.getNotation());
        callback.update("No wheel available - use Wheel Builder to generate", 0.0, 0, 0, 0);
        return null;
    }

    public List<int[]> buildAndSaveWheel(WheelParameters params, WheelProgressCallback callback, AtomicBoolean cancelled) {
        params.validate();

        logger.info("Building new wheel for {}", params.getNotation());

        List<int[]> wheel = optimizedBuilder.buildOptimalWheel(params, callback, cancelled);

        if (!cancelled.get() && wheel != null && !wheel.isEmpty()) {
            logger.info("Saving generated wheel to disk...");
            boolean saved = persistenceService.saveWheel(params, wheel);
            if (saved) {
                logger.info("Wheel saved successfully: wheels/pick{}/{}/{}_if_{}.txt",
                           params.getK(), params.getV(), params.getM(), params.getT());
            }
        }

        return wheel;
    }

    private List<int[]> constructTransversalDesign_15_5_3if5(WheelProgressCallback callback, AtomicBoolean cancelled) {
        callback.update("Using Transversal Design (15-5-3if5)...", 0.2, 0, 0, 0);

        List<int[]> wheel = new ArrayList<>();

        int[][] patterns = {
            {0,1,2,3,4}, {0,5,6,10,11}, {0,7,8,12,13}, {0,9,14,1,6},
            {0,2,7,11,14}, {1,5,7,9,12}, {1,8,10,13,14}, {2,5,8,9,13},
            {2,6,10,12,14}, {3,5,9,10,14}, {3,6,7,8,11}, {3,12,13,1,2},
            {4,5,11,12,13}, {4,6,7,9,14}, {4,8,10,1,3}, {6,8,9,0,3},
            {7,10,5,2,4}, {11,13,6,1,4}, {12,14,7,3,5}
        };

        for (int[] pattern : patterns) {
            if (cancelled.get()) break;
            int[] sorted = pattern.clone();
            Arrays.sort(sorted);
            wheel.add(sorted);
        }

        logger.info("Transversal Design wheel: {} lines", wheel.size());
        callback.update("Transversal Design complete", 0.9, 0, 0, wheel.size());
        return wheel;
    }
}
