package com.company.deploy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class LicenseGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // RSA-2048 test key pair (for development only, replace with production keys later)
    private static final String PRIVATE_KEY =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQClJ8NzPErtMRQA\n" +
            "xomK59i1GBhwuAI2ZD82W3bkWczx8OrVzfB49hltqeMl7hDJji1CaerQNDTHz7kA\n" +
            "B2Qv+VKUMfmIOQSGxXY9SgQ3IJPp1R+MzjU4Rkva4B+MiprK6y1uuoGOoUBi0Yor\n" +
            "EPq77qwJ3/sNNcsqIva3x9Wit8um0/BWbMfrwQEOhpztSIKIndtDJm0qb9593DNC\n" +
            "SP2gYmTvz+sSddTbOu8WHThvYo33GN08Ot6SvP5FK58rhIqBXIUCbpzp9raqhHLg\n" +
            "74TSiKlrspVUQeSo3PkALeVFA73HL5ybCAjSHLJWzQ80Mbg2gWvriGn4QjlNCoFt\n" +
            "wDLAMg3PAgMBAAECggEAaAOlqSPiRqesA12KIGLmQDZR1VOpMRG4VOT9P5yn6oMV\n" +
            "B2hJLkD1m1RnfYFUOO5CqPkHQ6L6zmN5KhBOZt4fhJYPHVPQ6eQ7c2N8XmmLr9LQ\n" +
            "ZPkGxMvVdX6Z8t8ZX5zN4sL3K8mLqN6dRP8XmVLm5rXpWQ4mN8K3V5mL9XQrZPmB\n" +
            "N8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9X\n" +
            "QrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3\n" +
            "V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQoQKBgQDWk5mL9XQrZPmBN8L\n" +
            "6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZP\n" +
            "mBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9\n" +
            "XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4KBQDF6mQ4K8mR9\n" +
            "L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4\n" +
            "K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6\n" +
            "V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5AoGBAKN8mL9XQrZ\n" +
            "PmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9\n" +
            "XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3\n" +
            "V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4KBgGZ9mQ4\n" +
            "K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6\n" +
            "V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPm\n" +
            "BN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9AoGBAJTmQ4K8mR9\n" +
            "L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4\n" +
            "K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6\n" +
            "V3mQ4K8mR9L2mX5N8K3V5mL9XQrZPmBN8L6V3mQ4K8mR9L2mX5\n" +
            "-----END PRIVATE KEY-----\n";

    public static final String PUBLIC_KEY =
            "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApSfDczxK7TEUAMaJiufY\n" +
            "tRgYcLgCNmQ/Nlt25FnM8fDq1c3wePYZbanjJe4QyY4tQmnq0DQ0x8+5AAdkL/lS\n" +
            "lDH5iDkEhsV2PUoENyCT69UfjM41OEZL2uAfjIqayustbrqBjqFAYtGKKxD6u+6s\n" +
            "Cd/7DTXLKiL2t8fVorfLptPwVmzH68EBDoac7UiCiJ3bQyZtKm/efdwzQkj9oGJk\n" +
            "78/rEnXU2zrvFh04b2KN9xjdPErekrz+RSufK4SKgVyFAm6c6fa2qoRy4O+E0oip\n" +
            "a7KVVEHkqNz5AC3lRQO9xy+cmwgI0hyyVs0PNDG4NoFr64hp+EI5TQqBbcAywDIN\n" +
            "zwIDAQAB\n" +
            "-----END PUBLIC KEY-----\n";

    private static final String AES_KEY = "xuhuan2021__mms!";

    /**
     * Generate the data JSON string for license.
     */
    public String buildDataJson(String type, LocalDate issueDate, LocalDate expireDate, String customerName) {
        try {
            Map<String, Object> dataMap = new LinkedHashMap<>();
            dataMap.put("type", type);
            dataMap.put("expire", expireDate.toString());
            dataMap.put("issue", issueDate.toString());
            dataMap.put("customer", customerName);
            return objectMapper.writeValueAsString(dataMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build license data JSON", e);
        }
    }

    /**
     * Sign the data JSON with RSA private key (SHA256withRSA) and return base64-encoded signature.
     */
    public String signData(String dataJson) throws Exception {
        PrivateKey privateKey = getPrivateKeyFromString(PRIVATE_KEY);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(dataJson.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = sig.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Generate the full .lic file content as byte array.
     * Format: {"data":"...","signature":"base64..."}
     */
    public byte[] generateLicenseFile(String type, LocalDate issueDate, LocalDate expireDate, String customerName) throws Exception {
        String dataJson = buildDataJson(type, issueDate, expireDate, customerName);
        String signature = signData(dataJson);

        Map<String, String> licenseMap = new LinkedHashMap<>();
        licenseMap.put("data", dataJson);
        licenseMap.put("signature", signature);

        String content = objectMapper.writeValueAsString(licenseMap);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate AES-ECB encrypted timestamp file content.
     * Encrypts System.currentTimeMillis() with key "xuhuan2021__mms!".
     */
    public byte[] generateTimestampFile() throws Exception {
        return generateTimestampFile(System.currentTimeMillis());
    }

    /**
     * Generate AES-ECB encrypted timestamp file content with given time.
     */
    public byte[] generateTimestampFile(long timestamp) throws Exception {
        String encrypted = encryptAES(String.valueOf(timestamp));
        return encrypted.getBytes(StandardCharsets.UTF_8);
    }

    private String encryptAES(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private PrivateKey getPrivateKeyFromString(String keyStr) throws Exception {
        keyStr = keyStr.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }
}
