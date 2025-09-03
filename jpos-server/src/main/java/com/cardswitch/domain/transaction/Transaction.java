package com.cardswitch.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class Transaction {
    private Long id;
    private String stan;              // System Trace Audit Number
    private String mti;               // Message Type Indicator
    private String panHash;
    private BigDecimal amount;
    private String acquirerId;
    private String terminalId;
    private String merchantId;
    private String responseCode;
    private String authCode;
    private LocalDateTime transactionTime;
    private LocalDate settlementDate;
    private TransactionStatus status;
    private String originalStan;      // For reversals
    private String currencyCode;

    // Constructors
    public Transaction() {}

    public Transaction(String mti, String stan) {
        this.mti = mti;
        this.stan = stan;
        this.transactionTime = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }

    public String getMti() { return mti; }
    public void setMti(String mti) { this.mti = mti; }

    public String getPanHash() { return panHash; }
    public void setPanHash(String panHash) { this.panHash = panHash; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getAcquirerId() { return acquirerId; }
    public void setAcquirerId(String acquirerId) { this.acquirerId = acquirerId; }

    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }

    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }

    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getOriginalStan() { return originalStan; }
    public void setOriginalStan(String originalStan) { this.originalStan = originalStan; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    // Helpers
    public boolean isReversal() {
        return "0400".equals(mti) || "0420".equals(mti);
    }

    public boolean isNetworkManagement() {
        return "0800".equals(mti);
    }
}
