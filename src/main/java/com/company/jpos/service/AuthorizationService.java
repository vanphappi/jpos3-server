package com.company.jpos.service;

import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;

@Service
public class AuthorizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    private final Random random = new Random();
    
    public String authorize(ISOMsg request, String correlationId) {
        try {
            String pan = request.getString(2);
            String amount = request.getString(4);
            
            logger.debug("Authorizing transaction - Correlation: {}, PAN: {}, Amount: {}", 
                        correlationId, maskPAN(pan), amount);
            
            // Basic validation
            if (pan == null || pan.length() < 13) {
                logger.warn("Invalid PAN - Correlation: {}", correlationId);
                return "14"; // Invalid account number
            }
            
            if (amount == null || amount.trim().isEmpty()) {
                logger.warn("Invalid amount - Correlation: {}", correlationId);
                return "13"; // Invalid amount
            }
            
            long amountValue = Long.parseLong(amount);
            if (amountValue <= 0) return "13";
            if (amountValue > 10000000) return "61"; // Exceeds limit
            
            // Business rules
            if (pan.endsWith("0000")) return "14"; // Invalid account
            
            String lastFour = pan.substring(Math.max(0, pan.length() - 4));
            if ("0001,0002,0003,0004,0005".contains(lastFour)) {
                return "51"; // Insufficient funds
            }
            
            // Random system error (1% chance)
            if (random.nextInt(100) == 0) return "96";
            
            logger.info("Transaction authorized - Correlation: {}", correlationId);
            return "00"; // Approved
            
        } catch (NumberFormatException e) {
            logger.error("Invalid amount format - Correlation: {}", correlationId, e);
            return "30"; // Format error
        } catch (Exception e) {
            logger.error("Authorization error - Correlation: {}", correlationId, e);
            return "96"; // System error
        }
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
}