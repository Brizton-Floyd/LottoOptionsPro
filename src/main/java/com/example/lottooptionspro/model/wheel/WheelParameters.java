package com.example.lottooptionspro.model.wheel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WheelParameters {
    
    private int v;
    private int k;
    private int t;
    private int m;
    
    public WheelParameters(int poolSize, int ticketSize, GuaranteeLevel guaranteeLevel) {
        this.v = poolSize;
        this.k = ticketSize;
        this.t = guaranteeLevel.getRequiredHits();
        this.m = guaranteeLevel.getGuaranteedMatches();
    }
    
    public void validate() {
        if (v < k) {
            throw new IllegalArgumentException("Pool size (v) must be >= ticket size (k)");
        }
        if (k < m) {
            throw new IllegalArgumentException("Ticket size (k) must be >= guarantee (m)");
        }
        if (t < m) {
            throw new IllegalArgumentException("Condition (t) must be >= guarantee (m)");
        }
        if (t > v) {
            throw new IllegalArgumentException("Condition (t) must be <= pool size (v)");
        }
        if (m < 1) {
            throw new IllegalArgumentException("Guarantee (m) must be at least 1");
        }
    }
    
    public String getNotation() {
        return String.format("C(%d,%d,%d,%d)", v, k, t, m);
    }
    
    public String getDescription() {
        return String.format("%d-if-%d guarantee for %d numbers (Pick-%d)", m, t, v, k);
    }
    
    public boolean isBalancedGuarantee() {
        return t == m;
    }
    
    public boolean isPrimePoolSize() {
        if (v < 2) return false;
        for (int i = 2; i <= Math.sqrt(v); i++) {
            if (v % i == 0) return false;
        }
        return true;
    }
}
