package com.example.lottooptionspro.util;

import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.models.GlobalOption;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom Gson deserializer to handle backward compatibility for GlobalOptions.
 * Supports both old format (Map<String, Coordinate>) and new format (List<GlobalOption>).
 */
public class GlobalOptionsDeserializer implements JsonDeserializer<List<GlobalOption>> {
    
    @Override
    public List<GlobalOption> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<GlobalOption> globalOptions = new ArrayList<>();
        
        if (json.isJsonArray()) {
            // New format: Array of GlobalOption objects
            JsonArray array = json.getAsJsonArray();
            for (JsonElement element : array) {
                GlobalOption go = context.deserialize(element, GlobalOption.class);
                globalOptions.add(go);
            }
        } else if (json.isJsonObject()) {
            // Old format: Object with name->coordinate mapping
            JsonObject object = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String name = entry.getKey();
                JsonElement coordElement = entry.getValue();
                
                if (coordElement.isJsonObject()) {
                    JsonObject coordObj = coordElement.getAsJsonObject();
                    double x = coordObj.get("x").getAsDouble();
                    double y = coordObj.get("y").getAsDouble();
                    
                    // Use default size for old format (no size information available)
                    double width = 20.0;  // Default width
                    double height = 20.0; // Default height
                    
                    GlobalOption go = new GlobalOption(name, x, y, width, height);
                    globalOptions.add(go);
                }
            }
        }
        
        return globalOptions;
    }
}