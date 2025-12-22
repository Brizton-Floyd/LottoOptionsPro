package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.GuaranteeLevel;
import com.example.lottooptionspro.model.wheel.WheelTable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class WheelTableService {
    
    private static final Logger logger = LoggerFactory.getLogger(WheelTableService.class);
    private final Map<String, WheelTable> wheelTables = new HashMap<>();
    private final Gson gson = new GsonBuilder().create();
    
    public WheelTableService() {
        loadWheelTables();
    }
    
    private void loadWheelTables() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:wheels/*.json");
            
            for (Resource resource : resources) {
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                    WheelTable table = gson.fromJson(reader, WheelTable.class);
                    String key = table.getKey();
                    wheelTables.put(key, table);
                    logger.info("Loaded wheel table: {} ({} lines)", key, table.getTotalLines());
                } catch (Exception e) {
                    logger.error("Failed to load wheel table: {}", resource.getFilename(), e);
                }
            }
            
            logger.info("Loaded {} wheel tables", wheelTables.size());
        } catch (Exception e) {
            logger.error("Failed to load wheel tables", e);
        }
    }
    
    public WheelTable getWheelTable(int poolSize, int pickSize, GuaranteeLevel guaranteeLevel) {
        String key = String.format("%d-%d-%s", poolSize, pickSize, guaranteeLevel.name());
        return wheelTables.get(key);
    }
    
    public boolean hasWheelTable(int poolSize, int pickSize, GuaranteeLevel guaranteeLevel) {
        String key = String.format("%d-%d-%s", poolSize, pickSize, guaranteeLevel.name());
        return wheelTables.containsKey(key);
    }
    
    public int getAvailableTableCount() {
        return wheelTables.size();
    }
}
