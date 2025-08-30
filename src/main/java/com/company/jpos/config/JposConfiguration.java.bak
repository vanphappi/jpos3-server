package com.company.jpos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Configuration
@ConfigurationProperties(prefix = "jpos")
@Validated
public class JposConfiguration {
    
    @NotNull
    private Server server = new Server();
    
    @NotNull
    private Database database = new Database();
    
    @NotNull
    private Security security = new Security();
    
    public static class Server {
        @Positive
        private int port = 8120;
        
        @NotNull
        private String host = "0.0.0.0";
        
        @Positive
        private int maxSessions = 1000;
        
        private boolean virtualThreads = true;
        
        // getters/setters
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getMaxSessions() { return maxSessions; }
        public void setMaxSessions(int maxSessions) { this.maxSessions = maxSessions; }
        public boolean isVirtualThreads() { return virtualThreads; }
        public void setVirtualThreads(boolean virtualThreads) { this.virtualThreads = virtualThreads; }
    }
    
    public static class Database {
        @NotNull
        private String url;
        
        @NotNull
        private String username;
        
        @NotNull
        private String password;
        
        @Positive
        private int maxConnections = 20;
        
        // getters/setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    }
    
    public static class Security {
        private boolean enabled = true;
        private String jwtSecret;
        private long jwtExpiration = 3600000; // 1 hour
        
        // getters/setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
        public long getJwtExpiration() { return jwtExpiration; }
        public void setJwtExpiration(long jwtExpiration) { this.jwtExpiration = jwtExpiration; }
    }
    
    // Main getters/setters
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
}