package com.cardswitch.application.service.impl;

import com.cardswitch.application.service.HSMService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class HSMServiceImpl implements HSMService {

    // Helper nhỏ cho encode/decode hex
    private static final HexFormat HF = HexFormat.of(); // mặc định lowercase
    private static String toHex(byte[] bytes) {
        // Trả về UPPERCASE cho tương thích với thói quen cũ của DatatypeConverter
        return HF.formatHex(bytes).toUpperCase();
    }
    private static byte[] fromHex(String hex) {
        return HF.parseHex(hex);
    }

    @Override
    public String generateKey() {
        byte[] key = new byte[16]; // 128-bit demo key
        new SecureRandom().nextBytes(key);
        return toHex(key);
    }

    @Override
    public String encrypt(String data, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(fromHex(key), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // demo ONLY
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String encryptedData, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(fromHex(key), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // demo ONLY
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(fromHex(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    @Override
    public boolean verifyPIN(String pan, String pinBlock, String key) {
        // Demo, KHÔNG dùng cho production
        try {
            String decryptedPinBlock = decrypt(pinBlock, key);
            return decryptedPinBlock.length() >= 4;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String generateMAC(String data, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(fromHex(key), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] macBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // Giữ hành vi cũ: lấy 16 ký tự đầu (8 byte) dạng HEX
            return toHex(macBytes).substring(0, 16);
        } catch (Exception e) {
            throw new RuntimeException("MAC generation failed", e);
        }
    }

    @Override
    public boolean verifyMAC(String data, String providedMac, String key) {
        try {
            String calculatedMac = generateMAC(data, key);
            return calculatedMac.equalsIgnoreCase(providedMac);
        } catch (Exception e) {
            return false;
        }
    }
}
