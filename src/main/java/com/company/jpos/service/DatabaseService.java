package com.company.jpos.service;

import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    // Simple in-memory storage for demo (replace with real DB in production)
    private final ConcurrentHashMap<String, TransactionRecord> transactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> reversals = new ConcurrentHashMap<>();
    
    public void recordTransaction(ISOMsg request, String correlationId) {
        try {
            String stan = request.getString(11);
            String mti = request.getMTI();
            String pan = request.getString(2);
            String amount = request.getString(4);
            
            TransactionRecord record = new TransactionRecord(
                stan, mti, pan, amount, correlationId, LocalDateTime.now()
            );
            
            transactions.put(stan, record);
            
            logger.info("Transaction recorded - STAN: {}, Correlation: {}", stan, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to record transaction - Correlation: {}", correlationId, e);
        }
    }
    
    public boolean reverseTransaction(String originalStan, String correlationId) {
        try {
            TransactionRecord original = transactions.get(originalStan);
            
            if (original == null) {
                logger.warn("Original transaction not found for reversal - STAN: {}, Correlation: {}", 
                           originalStan, correlationId);
                return false;
            }
            
            if (reversals.containsKey(originalStan)) {
                logger.warn("Transaction already reversed - STAN: {}, Correlation: {}", 
                           originalStan, correlationId);
                return false;
            }
            
            reversals.put(originalStan, true);
            
            logger.info("Transaction reversed - Original STAN: {}, Correlation: {}", 
                       originalStan, correlationId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to reverse transaction - STAN: {}, Correlation: {}", 
                        originalStan, correlationId, e);
            return false;
        }
    }
    
    public TransactionRecord getTransaction(String stan) {
        return transactions.get(stan);
    }
    
    public boolean isReversed(String stan) {
        return reversals.getOrDefault(stan, false);
    }
    
    public Map<String, TransactionRecord> getAllTransactions() {
        return new ConcurrentHashMap<>(transactions);
    }
    
    public int getTransactionCount() {
        return transactions.size();
    }
    
    // Inner class for transaction record
    public static class TransactionRecord {
        private final String stan;
        private final String mti;
        private final String pan;
        private final String amount;
        private final String correlationId;
        private final LocalDateTime timestamp;
        
        public TransactionRecord(String stan, String mti, String pan, String amount, 
                               String correlationId, LocalDateTime timestamp) {
            this.stan = stan;
            this.mti = mti;
            this.pan = pan;
            this.amount = amount;
            this.correlationId = correlationId;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getStan() { return stan; }
        public String getMti() { return mti; }
        public String getPan() { return pan; }
        public String getAmount() { return amount; }
        public String getCorrelationId() { return correlationId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("TxnRecord{stan='%s', mti='%s', amount='%s', time=%s}", 
                                stan, mti, amount, timestamp);
        }
    }
}