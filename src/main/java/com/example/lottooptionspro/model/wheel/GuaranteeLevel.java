package com.example.lottooptionspro.model.wheel;

public enum GuaranteeLevel {
    FIVE_IF_SIX(6, 5, 6, "5-if-6"),
    FOUR_IF_SIX(6, 4, 6, "4-if-6"),
    FOUR_IF_FIVE(6, 4, 5, "4-if-5"),
    FOUR_IF_FOUR(6, 4, 4, "4-if-4"),
    THREE_IF_SIX(6, 3, 6, "3-if-6"),
    THREE_IF_FIVE_P6(6, 3, 5, "3-if-5"),
    THREE_IF_FOUR_P6(6, 3, 4, "3-if-4"),
    THREE_IF_THREE_P6(6, 3, 3, "3-if-3"),
    
    FOUR_IF_FIVE_P5(5, 4, 5, "4-if-5"),
    FOUR_IF_FOUR_P5(5, 4, 4, "4-if-4"),
    THREE_IF_FIVE(5, 3, 5, "3-if-5"),
    THREE_IF_FOUR_P5(5, 3, 4, "3-if-4"),
    THREE_IF_THREE_P5(5, 3, 3, "3-if-3"),
    
    THREE_IF_FOUR(4, 3, 4, "3-if-4"),
    THREE_IF_THREE(4, 3, 3, "3-if-3"),
    TWO_IF_FOUR(4, 2, 4, "2-if-4"),
    TWO_IF_THREE(4, 2, 3, "2-if-3");

    private final int pickSize;
    private final int guaranteedMatches;
    private final int requiredHits;
    private final String displayName;

    GuaranteeLevel(int pickSize, int guaranteedMatches, int requiredHits, String displayName) {
        this.pickSize = pickSize;
        this.guaranteedMatches = guaranteedMatches;
        this.requiredHits = requiredHits;
        this.displayName = displayName;
    }

    public int getPickSize() {
        return pickSize;
    }

    public int getGuaranteedMatches() {
        return guaranteedMatches;
    }

    public int getRequiredHits() {
        return requiredHits;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GuaranteeLevel[] getAvailableGuaranteesForPickSize(int pickSize) {
        switch (pickSize) {
            case 6:
                return new GuaranteeLevel[]{FIVE_IF_SIX, FOUR_IF_SIX, FOUR_IF_FIVE, FOUR_IF_FOUR, 
                                           THREE_IF_SIX, THREE_IF_FIVE_P6, THREE_IF_FOUR_P6, THREE_IF_THREE_P6};
            case 5:
                return new GuaranteeLevel[]{FOUR_IF_FIVE_P5, FOUR_IF_FOUR_P5, THREE_IF_FIVE, THREE_IF_FOUR_P5, THREE_IF_THREE_P5};
            case 4:
                return new GuaranteeLevel[]{THREE_IF_FOUR, THREE_IF_THREE, TWO_IF_FOUR, TWO_IF_THREE};
            default:
                return new GuaranteeLevel[0];
        }
    }

    public static GuaranteeLevel fromMandT(int m, int t, int pickSize) {
        for (GuaranteeLevel level : values()) {
            if (level.pickSize == pickSize && 
                level.guaranteedMatches == m && 
                level.requiredHits == t) {
                return level;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
