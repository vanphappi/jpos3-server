package com.company.jpos.monitoring;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class HealthCheckController {
    
    private final DataSource dataSource;
    
    public HealthCheckController(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @GetMapping("/health")
    public Health health() {
        try {
            // Check database connectivity
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    return Health.up()
                            .withDetail("database", "UP")
                            .withDetail("virtualThreads", supportsVirtualThreads())
                            .withDetail("timestamp", System.currentTimeMillis())
                            .build();
                }
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "DOWN")
                    .withDetail("error", e.getMessage())
                    .build();
        }
        
        return Health.down().build();
    }
    
    @GetMapping("/ready")
    public Health readiness() {
        // Add readiness checks here
        return Health.up()
                .withDetail("status", "READY")
                .build();
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
