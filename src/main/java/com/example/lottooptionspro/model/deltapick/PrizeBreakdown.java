package com.example.lottooptionspro.model.deltapick;

/**
 * Breakdown of prizes by match level.
 */
public class PrizeBreakdown {
    private PrizeMatch match3;
    private PrizeMatch match4;
    private PrizeMatch match5;

    public PrizeBreakdown() {
    }

    public PrizeMatch getMatch3() {
        return match3;
    }

    public void setMatch3(PrizeMatch match3) {
        this.match3 = match3;
    }

    public PrizeMatch getMatch4() {
        return match4;
    }

    public void setMatch4(PrizeMatch match4) {
        this.match4 = match4;
    }

    public PrizeMatch getMatch5() {
        return match5;
    }

    public void setMatch5(PrizeMatch match5) {
        this.match5 = match5;
    }
}
