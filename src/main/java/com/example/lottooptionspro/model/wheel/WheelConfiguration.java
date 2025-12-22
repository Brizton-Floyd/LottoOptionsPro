package com.example.lottooptionspro.model.wheel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WheelConfiguration {
    private int poolSize;
    private int pickSize;
    private List<Integer> numbers;
    private GuaranteeLevel guaranteeLevel;

    public void validate() {
        if (poolSize < 4 || poolSize > 27) {
            throw new IllegalArgumentException("Pool size must be between 4 and 27");
        }
        if (pickSize < 4 || pickSize > 6) {
            throw new IllegalArgumentException("Pick size must be 4, 5, or 6");
        }
        if (numbers == null || numbers.size() != poolSize) {
            throw new IllegalArgumentException("Numbers list size must match pool size");
        }
        if (guaranteeLevel == null) {
            throw new IllegalArgumentException("Guarantee level must be specified");
        }
        if (guaranteeLevel.getPickSize() != pickSize) {
            throw new IllegalArgumentException("Guarantee level does not match pick size");
        }
    }
}
