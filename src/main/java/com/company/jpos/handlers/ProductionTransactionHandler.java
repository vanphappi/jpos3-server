package com.company.jpos.handlers;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.validation.constraints.NotNull;

@Component
public class ProductionTransactionHandler implements ISORequestListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductionTransactionHandler.class);
    
    private final Counter transactionCounter;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer transactionTimer;
    private final TransactionProcessor processor;
    private final AuditService auditService;
    
    public ProductionTransactionHandler(
            MeterRegistry meterRegistry,
            TransactionProcessor processor,
            AuditService auditService) {
        
        this.processor = processor;
        this.auditService = auditService;
        
        // Initialize metrics
        this.transactionCounter = Counter.builder("jpos.transactions.total")
                .description("Total number of transactions processed")
                .register(meterRegistry);
                
        this.successCounter = Counter.builder("jpos.transactions.success")
                .description("Number of successful transactions")
                .register(meterRegistry);
                
        this.errorCounter = Counter.builder("jpos.transactions.error")
                .description("Number of failed transactions")
                .register(meterRegistry);
                
        this.transactionTimer = Timer.builder("jpos.transaction.duration")
                .description("Transaction processing time")
                .register(meterRegistry);
        
        logger.info("Production Transaction Handler initialized with Virtual Threads: {}", 
                   supportsVirtualThreads());
    }
    
    @Override
    public boolean process(@NotNull ISOSource source, @NotNull ISOMsg request) {
        String correlationId = UUID.randomUUID().toString();
        
        // Set correlation ID for distributed tracing
        MDC.put("correlationId", correlationId);
        
        try {
            transactionCounter.increment();
            
            if (supportsVirtualThreads()) {
                Thread.startVirtualThread(() -> processTransaction(source, request, correlationId));
            } else {
                CompletableFuture.runAsync(() -> processTransaction(source, request, correlationId));
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to process transaction", e);
            errorCounter.increment();
            return false;
        } finally {
            MDC.clear();
        }
    }
    
    private void processTransaction(ISOSource source, ISOMsg request, String correlationId) {
        Timer.Sample timer = Timer.start();
        
        try {
            MDC.put("correlationId", correlationId);
            
            // Extract transaction info
            String mti = request.getMTI();
            String stan = request.getString(11);
            String pan = maskPAN(request.getString(2));
            
            logger.info("Processing transaction - MTI: {}, STAN: {}, PAN: {}, Thread: {}", 
                       mti, stan, pan, Thread.currentThread().isVirtual() ? "Virtual" : "Platform");
            
            // Process transaction
            ISOMsg response = processor.process(request, correlationId);
            
            if (response != null) {
                // Send response
                source.send(response);
                
                // Audit logging
                auditService.logTransaction(request, response, correlationId);
                
                successCounter.increment();
                logger.info("Transaction completed successfully - STAN: {}, Response: {}", 
                           stan, response.getString(39));
            } else {
                errorCounter.increment();
                logger.error("Transaction processing failed - STAN: {}", stan);
            }
            
        } catch (Exception e) {
            errorCounter.increment();
            logger.error("Transaction processing error - Correlation: {}", correlationId, e);
            
            try {
                // Send error response
                ISOMsg errorResponse = createErrorResponse(request);
                source.send(errorResponse);
            } catch (Exception sendException) {
                logger.error("Failed to send error response", sendException);
            }
            
        } finally {
            timer.stop(transactionTimer);
            MDC.clear();
        }
    }
    
    private ISOMsg createErrorResponse(ISOMsg request) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        String responseMTI = request.getMTI().substring(0, 2) + "10";
        response.setMTI(responseMTI);
        response.set(39, "96"); // System error
        return response;
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
    
    private boolean supportsVirtualThreads() {
        try {
            Thread.class.getMethod("startVirtualThread", Runnable.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
