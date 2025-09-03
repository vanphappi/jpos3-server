package com.cardswitch.domain.routing;

public class RoutingRule {
    private String mti;
    private String processingCode;
    private String acquirerId;
    private String terminalType;
    private String destination;
    private int priority;
    private boolean active;
    
    public boolean matches(String mti, String processingCode, String acquirerId) {
        return (this.mti == null || this.mti.equals(mti)) &&
               (this.processingCode == null || this.processingCode.equals(processingCode)) &&
               (this.acquirerId == null || this.acquirerId.equals(acquirerId)) &&
               this.active;
    }
    
    // Getters and setters...
    public String getDestination() { return destination; }
    public int getPriority() { return priority; }
}
