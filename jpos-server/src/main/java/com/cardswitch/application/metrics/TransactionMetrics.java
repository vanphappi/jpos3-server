package com.cardswitch.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TransactionMetrics {
    
    private final Counter transactionCounter;
    private final Counter approvedCounter;
    private final Counter declinedCounter;
    private final Timer transactionTimer;
    private final Counter fraudCounter;
    
    public TransactionMetrics(MeterRegistry meterRegistry) {
        this.transactionCounter = Counter.builder("switch.transactions.total")
            .description("Total number of transactions processed")
            .register(meterRegistry);
            
        this.approvedCounter = Counter.builder("switch.transactions.approved")
            .description("Number of approved transactions")
            .register(meterRegistry);
            
        this.declinedCounter = Counter.builder("switch.transactions.declined")
            .description("Number of declined transactions")
            .register(meterRegistry);
            
        this.transactionTimer = Timer.builder("switch.transaction.duration")
            .description("Transaction processing duration")
            .register(meterRegistry);
            
        this.fraudCounter = Counter.builder("switch.fraud.detected")
            .description("Number of fraud cases detected")
            .register(meterRegistry);
    }
    
    public void incrementTransactionCount() {
        transactionCounter.increment();
    }
    
    public void incrementApprovedCount() {
        approvedCounter.increment();
    }
    
    public void incrementDeclinedCount() {
        declinedCounter.increment();
    }
    
    public void incrementFraudCount() {
        fraudCounter.increment();
    }
    
    public Timer.Sample startTransactionTimer() {
        return Timer.start();
    }
    
    public void stopTransactionTimer(Timer.Sample sample) {
        sample.stop(transactionTimer);
    }
}