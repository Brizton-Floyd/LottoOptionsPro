package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.wheel.GuaranteeLevel;
import com.example.lottooptionspro.model.wincheck.WinCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WinCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(WinCheckService.class);
    
    public WinCheckResult checkWins(List<int[]> wheelTickets, int[] winningNumbers, GuaranteeLevel guarantee) {
        logger.info("Checking {} tickets against winning numbers: {}", 
                   wheelTickets.size(), Arrays.toString(winningNumbers));
        
        int pickSize = winningNumbers.length;
        Set<Integer> winningSet = Arrays.stream(winningNumbers).boxed().collect(Collectors.toSet());
        
        List<WinCheckResult.TicketMatch> matches = new ArrayList<>();
        Map<Integer, Integer> matchCountMap = new HashMap<>();
        
        for (int i = 0; i < wheelTickets.size(); i++) {
            int[] ticket = wheelTickets.get(i);
            List<Integer> matchedNumbers = new ArrayList<>();
            
            for (int num : ticket) {
                if (winningSet.contains(num)) {
                    matchedNumbers.add(num);
                }
            }
            
            int matchCount = matchedNumbers.size();
            
            if (matchCount >= 2) {
                matches.add(new WinCheckResult.TicketMatch(i + 1, ticket, matchCount, matchedNumbers));
                matchCountMap.put(matchCount, matchCountMap.getOrDefault(matchCount, 0) + 1);
            }
        }
        
        matches.sort((a, b) -> Integer.compare(b.getMatchCount(), a.getMatchCount()));
        
        int winningTickets = matches.size();
        int perfectMatches = matchCountMap.getOrDefault(pickSize, 0);
        int match5 = matchCountMap.getOrDefault(pickSize - 1, 0);
        int match4 = matchCountMap.getOrDefault(pickSize - 2, 0);
        int match3 = matchCountMap.getOrDefault(pickSize - 3, 0);
        int match2 = matchCountMap.getOrDefault(2, 0);
        
        boolean guaranteeVerified = false;
        String guaranteeMessage = "";
        
        if (guarantee != null) {
            guaranteeVerified = verifyGuarantee(winningNumbers, wheelTickets, guarantee);
            guaranteeMessage = buildGuaranteeMessage(guarantee, guaranteeVerified, matchCountMap, pickSize);
        }
        
        WinCheckResult.WinSummary summary = new WinCheckResult.WinSummary(
            wheelTickets.size(),
            winningTickets,
            perfectMatches,
            match5,
            match4,
            match3,
            match2,
            guaranteeVerified,
            guaranteeMessage
        );
        
        logger.info("Win check complete: {} winning tickets out of {}", winningTickets, wheelTickets.size());
        logger.info("Breakdown - {}/{}: {}, {}/{}: {}, {}/{}: {}, {}/{}: {}, 2/{}: {}", 
                   pickSize, pickSize, perfectMatches,
                   pickSize-1, pickSize, match5,
                   pickSize-2, pickSize, match4,
                   pickSize-3, pickSize, match3,
                   pickSize, match2);
        
        return new WinCheckResult(winningNumbers, matches, summary);
    }
    
    private boolean verifyGuarantee(int[] winningNumbers, List<int[]> wheelTickets, GuaranteeLevel guarantee) {
        int requiredHits = guarantee.getRequiredHits();
        int guaranteedMatches = guarantee.getGuaranteedMatches();

        Set<Integer> winningSet = Arrays.stream(winningNumbers).boxed().collect(Collectors.toSet());

        Set<Integer> wheelPool = wheelTickets.stream()
                .flatMap(ticket -> Arrays.stream(ticket).boxed())
                .collect(Collectors.toSet());
        long hitsInPool = winningSet.stream().filter(wheelPool::contains).count();

        if (hitsInPool < requiredHits) {
            return false;
        }

        for (int[] ticket : wheelTickets) {
            int matchCount = 0;
            for (int num : ticket) {
                if (winningSet.contains(num)) {
                    matchCount++;
                }
            }

            if (matchCount >= guaranteedMatches) {
                return true;
            }
        }

        return false;
    }
    
    private String buildGuaranteeMessage(GuaranteeLevel guarantee, boolean verified,
                                        Map<Integer, Integer> matchCountMap, int pickSize) {
        StringBuilder msg = new StringBuilder();
        msg.append("Guarantee: ").append(guarantee.getDisplayName()).append(" - ");
        
        if (verified) {
            msg.append("✓ VERIFIED");
            
            if (matchCountMap.getOrDefault(pickSize, 0) > 0) {
                msg.append(" (JACKPOT!)");
            } else {
                for (int i = pickSize - 1; i >= guarantee.getGuaranteedMatches(); i--) {
                    if (matchCountMap.getOrDefault(i, 0) > 0) {
                        msg.append(" (").append(i).append(" matches achieved)");
                        break;
                    }
                }
            }
        } else {
            msg.append("✗ Not verified (winning numbers may not be in wheel pool)");
        }
        
        return msg.toString();
    }
}
