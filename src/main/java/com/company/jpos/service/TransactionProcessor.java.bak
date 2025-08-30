package com.company.jpos.service;

import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;

@Service
public class TransactionProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    
    private final AuthorizationService authService;
    private final FraudDetectionService fraudService;
    private final DatabaseService databaseService;
    
    public TransactionProcessor(
            AuthorizationService authService,
            FraudDetectionService fraudService,
            DatabaseService databaseService) {
        
        this.authService = authService;
        this.fraudService = fraudService;
        this.databaseService = databaseService;
    }
    
    @CircuitBreaker(name = "transaction-processor", fallbackMethod = "fallbackProcess")
    @RateLimiter(name = "transaction-processor")
    @Retry(name = "transaction-processor")
    public ISOMsg process(ISOMsg request, String correlationId) throws Exception {
        
        String mti = request.getMTI();
        
        switch (mti) {
            case "0200": // Financial request
                return processFinancialTransaction(request, correlationId);
                
            case "0420": // Reversal
                return processReversal(request, correlationId);
                
            case "0800": // Network management
                return processNetworkTest(request, correlationId);
                
            default:
                return createUnsupportedResponse(request);
        }
    }
    
    private ISOMsg processFinancialTransaction(ISOMsg request, String correlationId) throws Exception {
        logger.debug("Processing financial transaction - Correlation: {}", correlationId);
        
        // 1. Fraud detection
        if (fraudService.isSuspicious(request)) {
            logger.warn("Suspicious transaction detected - Correlation: {}", correlationId);
            return createDeclinedResponse(request, "57"); // Suspected fraud
        }
        
        // 2. Authorization
        String authResult = authService.authorize(request, correlationId);
        
        // 3. Database operations
        if ("00".equals(authResult)) {
            databaseService.recordTransaction(request, correlationId);
        }
        
        // 4. Create response
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0210");
        response.set(39, authResult);
        response.set(38, generateApprovalCode());
        
        return response;
    }
    
    private ISOMsg processReversal(ISOMsg request, String correlationId) throws Exception {
        logger.debug("Processing reversal - Correlation: {}", correlationId);
        
        String originalStan = request.getString(11);
        boolean reversed = databaseService.reverseTransaction(originalStan, correlationId);
        
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0430");
        response.set(39, reversed ? "00" : "25");
        
        return response;
    }
    
    private ISOMsg processNetworkTest(ISOMsg request, String correlationId) throws Exception {
        logger.debug("Processing network test - Correlation: {}", correlationId);
        
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0810");
        response.set(39, "00");
        
        return response;
    }
    
    private ISOMsg createUnsupportedResponse(ISOMsg request) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        String responseMTI = request.getMTI().substring(0, 2) + "10";
        response.setMTI(responseMTI);
        response.set(39, "12"); // Invalid transaction
        return response;
    }
    
    private ISOMsg createDeclinedResponse(ISOMsg request, String responseCode) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0210");
        response.set(39, responseCode);
        return response;
    }
    
    @Cacheable(value = "approval-codes", key = "#root.methodName")
    private String generateApprovalCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
    
    // Fallback method for circuit breaker
    public ISOMsg fallbackProcess(ISOMsg request, String correlationId, Exception ex) throws Exception {
        logger.error("Circuit breaker activated for correlation: {}", correlationId, ex);
        
        ISOMsg response = (ISOMsg) request.clone();
        String responseMTI = request.getMTI().substring(0, 2) + "10";
        response.setMTI(responseMTI);
        response.set(39, "96"); // System error
        
        return response;
    }
}
