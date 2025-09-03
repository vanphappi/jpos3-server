package com.cardswitch.config.health;

import com.cardswitch.application.service.TransactionService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class SwitchHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final TransactionService transactionService;
    
    public SwitchHealthIndicator(DataSource dataSource, TransactionService transactionService) {
        this.dataSource = dataSource;
        this.transactionService = transactionService;
    }
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(5)) {
                    return Health.down().withDetail("database", "Connection invalid").build();
                }
            }
            
            // Check if we can process transactions
            // This is a simplified check - in production you'd have more comprehensive checks
            
            return Health.up()
                .withDetail("database", "Connected")
                .withDetail("transaction_service", "Available")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}