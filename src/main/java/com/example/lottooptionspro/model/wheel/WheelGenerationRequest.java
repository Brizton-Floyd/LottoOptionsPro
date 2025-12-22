package com.example.lottooptionspro.model.wheel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WheelGenerationRequest {
    private WheelConfiguration configuration;
}
