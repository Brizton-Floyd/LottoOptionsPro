package com.example.lottooptionspro.model.wheel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WheelTable {
    private int poolSize;
    private int pickSize;
    private GuaranteeLevel guaranteeLevel;
    private int totalLines;
    private String source;
    private boolean verified;
    private List<int[]> patterns;
    
    public String getKey() {
        return String.format("%d-%d-%s", poolSize, pickSize, guaranteeLevel.name());
    }
}
