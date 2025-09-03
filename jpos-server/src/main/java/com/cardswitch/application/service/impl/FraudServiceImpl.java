package com.cardswitch.application.service.impl;

import com.cardswitch.application.service.FraudService;
import com.cardswitch.domain.transaction.Transaction;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class FraudServiceImpl implements FraudService {
    
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 5;
    
    @Override
    public boolean isSuspicious(Transaction transaction) {
        // High amount check
        if (transaction.getAmount() != null && 
            transaction.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            return true;
        }
        
        // Velocity check (simplified - in production use Redis)
        if (isHighVelocity(transaction)) {
            return true;
        }
        
        // Time-based checks
        if (isUnusualTime(transaction)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isHighVelocity(Transaction transaction) {
        // In production, query recent transactions for same PAN hash
        // For now, return false
        return false;
    }
    
    private boolean isUnusualTime(Transaction transaction) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Flag transactions between 2 AM and 5 AM as suspicious
        return hour >= 2 && hour <= 5;
    }
}