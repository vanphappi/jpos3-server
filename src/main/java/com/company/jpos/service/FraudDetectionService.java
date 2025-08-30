package com.company.jpos.service;

import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FraudDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);
    
    // Simple velocity checking
    private final ConcurrentHashMap<String, AtomicInteger> panCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastResetTime = new ConcurrentHashMap<>();
    
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 20;
    private static final long RESET_INTERVAL = 60000; // 1 minute
    
    public boolean isSuspicious(ISOMsg request) {
        try {
            String pan = request.getString(2);
            String amount = request.getString(4);
            
            if (pan == null || amount == null) return false;
            
            // Check for suspicious patterns
            if (checkVelocity(pan)) {
                logger.warn("Velocity fraud detected - PAN: {}", maskPAN(pan));
                return true;
            }
            
            if (checkAmountPattern(amount)) {
                logger.warn("Suspicious amount pattern - Amount: {}", amount);
                return true;
            }
            
            if (checkTimePattern()) {
                logger.warn("Suspicious time pattern detected");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Fraud detection error", e);
            return false; // Don't block on fraud check errors
        }
    }
    
    private boolean checkVelocity(String pan) {
        String maskedPan = maskPAN(pan);
        long currentTime = System.currentTimeMillis();
        
        // Reset counter if needed
        Long lastReset = lastResetTime.get(maskedPan);
        if (lastReset == null || (currentTime - lastReset) > RESET_INTERVAL) {
            panCounters.put(maskedPan, new AtomicInteger(0));
            lastResetTime.put(maskedPan, currentTime);
        }
        
        // Check velocity
        AtomicInteger counter = panCounters.computeIfAbsent(maskedPan, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        
        return count > MAX_TRANSACTIONS_PER_MINUTE;
    }
    
    private boolean checkAmountPattern(String amount) {
        try {
            long amountValue = Long.parseLong(amount);
            
            // Flag very round amounts over certain threshold
            if (amountValue > 1000000 && amountValue % 100000 == 0) {
                return true; // Suspicious round amount
            }
            
            // Flag very high amounts
            if (amountValue > 50000000) { // > 500,000
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkTimePattern() {
        // Simple time-based fraud check
        int hour = java.time.LocalTime.now().getHour();
        
        // Flag transactions between 2-4 AM (higher fraud risk)
        return hour >= 2 && hour < 4;
    }
    
    @Cacheable(value = "fraud-pan-cache", key = "#pan")
    public boolean isPANBlacklisted(String pan) {
        // In real implementation, check against fraud database
        String lastFour = pan.substring(Math.max(0, pan.length() - 4));
        return "9999,8888,7777".contains(lastFour);
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
}
