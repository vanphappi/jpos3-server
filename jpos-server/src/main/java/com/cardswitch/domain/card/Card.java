package com.cardswitch.domain.card;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class Card {
    private String pan;
    private String panHash;
    private String bin;
    private CardType cardType;
    private String issuerId;
    private CardStatus status;
    
    public Card(String pan) {
        this.pan = pan;
        this.panHash = hashPan(pan);
        this.bin = pan.substring(0, 6);
    }
    
    private String hashPan(String pan) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pan.getBytes(StandardCharsets.UTF_8));
            
            // convert byte[] → hex string
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có trong JRE → nếu thiếu thì lỗi runtime
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
    
    // Getters and setters...
    public String getBin() { return bin; }
    public CardType getCardType() { return cardType; }
    public String getIssuerId() { return issuerId; }
}

enum CardType {
    VISA, MASTERCARD, AMEX, DOMESTIC
}

enum CardStatus {
    ACTIVE, BLOCKED, EXPIRED, CLOSED
}
