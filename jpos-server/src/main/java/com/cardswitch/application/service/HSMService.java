package com.cardswitch.application.service;

public interface HSMService {
    String generateKey();
    String encrypt(String data, String key);
    String decrypt(String encryptedData, String key);
    boolean verifyPIN(String pan, String pinBlock, String key);
    String generateMAC(String data, String key);
    boolean verifyMAC(String data, String mac, String key);
}