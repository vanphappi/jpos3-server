package com.cardswitch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "switch")
public class SwitchConfiguration {
    
    private Core core = new Core();
    private Security security = new Security();
    private Database database = new Database();
    private Monitoring monitoring = new Monitoring();
    
    public static class Core {
        private int maxSessions = 100;
        private long transactionTimeout = 30000;
        private int retryAttempts = 3;
        private int serverPort = 9150;
        
        // Getters and setters
        public int getMaxSessions() { return maxSessions; }
        public void setMaxSessions(int maxSessions) { this.maxSessions = maxSessions; }
        
        public long getTransactionTimeout() { return transactionTimeout; }
        public void setTransactionTimeout(long transactionTimeout) { this.transactionTimeout = transactionTimeout; }
        
        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
        
        public int getServerPort() { return serverPort; }
        public void setServerPort(int serverPort) { this.serverPort = serverPort; }
    }
    
    public static class Security {
        private boolean hsmEnabled = false;
        private String hsmHost = "localhost";
        private int hsmPort = 1500;
        private boolean pinVerificationEnabled = true;
        private boolean macVerificationEnabled = true;
        
        // Getters and setters
        public boolean isHsmEnabled() { return hsmEnabled; }
        public void setHsmEnabled(boolean hsmEnabled) { this.hsmEnabled = hsmEnabled; }
        
        public String getHsmHost() { return hsmHost; }
        public void setHsmHost(String hsmHost) { this.hsmHost = hsmHost; }
        
        public int getHsmPort() { return hsmPort; }
        public void setHsmPort(int hsmPort) { this.hsmPort = hsmPort; }
    }
    
    public static class Database {
        private int poolSize = 20;
        private long connectionTimeout = 5000;
        private boolean enableAuditLog = true;
        
        // Getters and setters
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    }
    
    public static class Monitoring {
        private boolean metricsEnabled = true;
        private int alertThresholdTps = 1000;
        private double alertThresholdResponseTime = 3.0;
        private double alertThresholdSuccessRate = 99.5;
        
        // Getters and setters
        public boolean isMetricsEnabled() { return metricsEnabled; }
        public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }
    }
    
    // Main getters
    public Core getCore() { return core; }
    public void setCore(Core core) { this.core = core; }
    
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }
    
    public Monitoring getMonitoring() { return monitoring; }
    public void setMonitoring(Monitoring monitoring) { this.monitoring = monitoring; }
}
