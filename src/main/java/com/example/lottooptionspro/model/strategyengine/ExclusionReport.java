package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExclusionReport {
    private Integer number;
    private String reason;
    private Integer currentSkip;
    private Integer limit;
    private String note;
}
