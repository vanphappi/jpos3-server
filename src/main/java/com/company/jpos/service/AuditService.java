package com.company.jpos.service;

import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class AuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    // Simple in-memory audit trail (replace with database in production)
    private final ConcurrentLinkedQueue<AuditRecord> auditTrail = new ConcurrentLinkedQueue<>();
    private static final int MAX_AUDIT_RECORDS = 10000;
    
    public void logTransaction(ISOMsg request, ISOMsg response, String correlationId) {
        try {
            AuditRecord record = createAuditRecord(request, response, correlationId);
            
            // Add to audit trail
            auditTrail.offer(record);
            
            // Keep only recent records
            while (auditTrail.size() > MAX_AUDIT_RECORDS) {
                auditTrail.poll();
            }
            
            // Log to audit logger
            auditLogger.info("TXN|{}|{}|{}|{}|{}|{}|{}", 
                record.getCorrelationId(),
                record.getRequestMti(),
                record.getStan(),
                record.getMaskedPan(),
                record.getAmount(),
                record.getResponseCode(),
                record.getTimestamp()
            );
            
            logger.debug("Transaction audited - Correlation: {}", correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to audit transaction - Correlation: {}", correlationId, e);
        }
    }
    
    public void logSecurityEvent(String eventType, String details, String correlationId) {
        try {
            auditLogger.warn("SECURITY|{}|{}|{}|{}", 
                eventType, details, correlationId, LocalDateTime.now());
                
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        }
    }
    
    public void logSystemEvent(String eventType, String details) {
        try {
            auditLogger.info("SYSTEM|{}|{}|{}", 
                eventType, details, LocalDateTime.now());
                
        } catch (Exception e) {
            logger.error("Failed to log system event", e);
        }
    }
    
    private AuditRecord createAuditRecord(ISOMsg request, ISOMsg response, String correlationId) {
        return new AuditRecord(
            correlationId,
            request.getMTI(),
            request.getString(11), // STAN
            maskPAN(request.getString(2)), // PAN
            request.getString(4), // Amount
            response.getString(39), // Response code
            LocalDateTime.now()
        );
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
    
    public ConcurrentLinkedQueue<AuditRecord> getAuditTrail() {
        return new ConcurrentLinkedQueue<>(auditTrail);
    }
    
    public long getAuditRecordCount() {
        return auditTrail.size();
    }
    
    // Inner class for audit record
    public static class AuditRecord {
        private final String correlationId;
        private final String requestMti;
        private final String stan;
        private final String maskedPan;
        private final String amount;
        private final String responseCode;
        private final LocalDateTime timestamp;
        
        public AuditRecord(String correlationId, String requestMti, String stan, 
                          String maskedPan, String amount, String responseCode, 
                          LocalDateTime timestamp) {
            this.correlationId = correlationId;
            this.requestMti = requestMti;
            this.stan = stan;
            this.maskedPan = maskedPan;
            this.amount = amount;
            this.responseCode = responseCode;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getCorrelationId() { return correlationId; }
        public String getRequestMti() { return requestMti; }
        public String getStan() { return stan; }
        public String getMaskedPan() { return maskedPan; }
        public String getAmount() { return amount; }
        public String getResponseCode() { return responseCode; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("AuditRecord{correlation='%s', mti='%s', stan='%s', response='%s', time=%s}", 
                                correlationId, requestMti, stan, responseCode, 
                                timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }
}
