package com.example.lottooptionspro.model.deltapick;

/**
 * Enum representing the delta input mode for pick generation.
 */
public enum DeltaInputMode {
    /**
     * Raw delta mode - deltas are used in their original sequential order.
     */
    RAW,
    
    /**
     * Sorted delta mode - deltas are sorted by magnitude before processing.
     */
    SORTED
}
