package com.example.lottooptionspro.model.deltapick;

import com.google.gson.annotations.SerializedName;

/**
 * Response from the game configuration API endpoint.
 * The API returns a nested structure with lotteryGame object.
 */
public class GameConfigResponse {
    @SerializedName("lotteryGame")
    private LotteryGameInfo lotteryGame;

    public GameConfigResponse() {
    }

    public Integer getMaxNumber() {
        return lotteryGame != null ? lotteryGame.maxNumber : null;
    }

    public void setMaxNumber(Integer maxNumber) {
        if (lotteryGame == null) {
            lotteryGame = new LotteryGameInfo();
        }
        lotteryGame.maxNumber = maxNumber;
    }

    public Integer getDrawPositionCount() {
        return lotteryGame != null ? lotteryGame.drawPositionCount : null;
    }

    public void setDrawPositionCount(Integer drawPositionCount) {
        if (lotteryGame == null) {
            lotteryGame = new LotteryGameInfo();
        }
        lotteryGame.drawPositionCount = drawPositionCount;
    }
    
    public LotteryGameInfo getLotteryGame() {
        return lotteryGame;
    }
    
    public void setLotteryGame(LotteryGameInfo lotteryGame) {
        this.lotteryGame = lotteryGame;
    }

    /**
     * Inner class representing the lotteryGame object from the API.
     */
    public static class LotteryGameInfo {
        private String fullName;
        private String stateGameBelongsTo;
        private String dateLastUpdated;
        private Integer drawHistoryCount;
        private Integer drawPositionCount;
        private Integer maxNumber;
        private Integer minNumber;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getStateGameBelongsTo() {
            return stateGameBelongsTo;
        }

        public void setStateGameBelongsTo(String stateGameBelongsTo) {
            this.stateGameBelongsTo = stateGameBelongsTo;
        }

        public String getDateLastUpdated() {
            return dateLastUpdated;
        }

        public void setDateLastUpdated(String dateLastUpdated) {
            this.dateLastUpdated = dateLastUpdated;
        }

        public Integer getDrawHistoryCount() {
            return drawHistoryCount;
        }

        public void setDrawHistoryCount(Integer drawHistoryCount) {
            this.drawHistoryCount = drawHistoryCount;
        }

        public Integer getDrawPositionCount() {
            return drawPositionCount;
        }

        public void setDrawPositionCount(Integer drawPositionCount) {
            this.drawPositionCount = drawPositionCount;
        }

        public Integer getMaxNumber() {
            return maxNumber;
        }

        public void setMaxNumber(Integer maxNumber) {
            this.maxNumber = maxNumber;
        }

        public Integer getMinNumber() {
            return minNumber;
        }

        public void setMinNumber(Integer minNumber) {
            this.minNumber = minNumber;
        }
    }
}
