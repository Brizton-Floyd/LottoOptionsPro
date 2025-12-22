package com.example.lottooptionspro.model.wincheck;

import java.util.List;

public class WinCheckResult {
    private final int[] winningNumbers;
    private final List<TicketMatch> matches;
    private final WinSummary summary;
    
    public WinCheckResult(int[] winningNumbers, List<TicketMatch> matches, WinSummary summary) {
        this.winningNumbers = winningNumbers;
        this.matches = matches;
        this.summary = summary;
    }
    
    public int[] getWinningNumbers() {
        return winningNumbers;
    }
    
    public List<TicketMatch> getMatches() {
        return matches;
    }
    
    public WinSummary getSummary() {
        return summary;
    }
    
    public static class TicketMatch {
        private final int ticketNumber;
        private final int[] ticketNumbers;
        private final int matchCount;
        private final List<Integer> matchedNumbers;
        
        public TicketMatch(int ticketNumber, int[] ticketNumbers, int matchCount, List<Integer> matchedNumbers) {
            this.ticketNumber = ticketNumber;
            this.ticketNumbers = ticketNumbers;
            this.matchCount = matchCount;
            this.matchedNumbers = matchedNumbers;
        }
        
        public int getTicketNumber() {
            return ticketNumber;
        }
        
        public int[] getTicketNumbers() {
            return ticketNumbers;
        }
        
        public int getMatchCount() {
            return matchCount;
        }
        
        public List<Integer> getMatchedNumbers() {
            return matchedNumbers;
        }
    }
    
    public static class WinSummary {
        private final int totalTickets;
        private final int winningTickets;
        private final int perfectMatches;
        private final int match5;
        private final int match4;
        private final int match3;
        private final int match2;
        private final boolean guaranteeVerified;
        private final String guaranteeMessage;
        
        public WinSummary(int totalTickets, int winningTickets, int perfectMatches, 
                         int match5, int match4, int match3, int match2,
                         boolean guaranteeVerified, String guaranteeMessage) {
            this.totalTickets = totalTickets;
            this.winningTickets = winningTickets;
            this.perfectMatches = perfectMatches;
            this.match5 = match5;
            this.match4 = match4;
            this.match3 = match3;
            this.match2 = match2;
            this.guaranteeVerified = guaranteeVerified;
            this.guaranteeMessage = guaranteeMessage;
        }
        
        public int getTotalTickets() {
            return totalTickets;
        }
        
        public int getWinningTickets() {
            return winningTickets;
        }
        
        public int getPerfectMatches() {
            return perfectMatches;
        }
        
        public int getMatch5() {
            return match5;
        }
        
        public int getMatch4() {
            return match4;
        }
        
        public int getMatch3() {
            return match3;
        }
        
        public int getMatch2() {
            return match2;
        }
        
        public boolean isGuaranteeVerified() {
            return guaranteeVerified;
        }
        
        public String getGuaranteeMessage() {
            return guaranteeMessage;
        }
    }
}
