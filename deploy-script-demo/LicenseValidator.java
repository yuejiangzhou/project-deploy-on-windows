package org.unreal.model.util;

import com.alibaba.fastjson.JSONObject;
import org.unreal.model.constant.LicenseConstants;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

public class LicenseValidator {

    private static final String LICENSE_PATH = "./license/license.lic";
    private static final long ALLOWED_DRIFT_MS = 60000;
//    private static final ObjectMapper mapper = new ObjectMapper();

    public static void validateOrExit() {
        try {
            // ===== 第一步：强制读取时间锚点（缺失即抛异常） =====
            long currentTime = System.currentTimeMillis();
            long lastValidTime;
            try {
                lastValidTime = TimeTracker.getLastValidTime();
            } catch (Exception e) {
                System.err.println("LICENSE ERROR: " + e.getMessage());
                System.exit(-1);
                return;
            }

            // ===== 第二步：防时间回拨检测 =====
            if (lastValidTime != 0 && currentTime < lastValidTime - ALLOWED_DRIFT_MS) {
                throw new RuntimeException("System time rollback detected! (Last: " + lastValidTime + ", Now: " + currentTime + ")");
            }

            // ===== 第三步：读取并验签 License 文件 =====
            File licenseFile = new File(LICENSE_PATH);
            if (!licenseFile.exists()) {
                throw new RuntimeException("License file not found! (./license/license.lic)");
            }

            byte[] bytes = Files.readAllBytes(licenseFile.toPath());
            String content = new String(bytes, StandardCharsets.UTF_8);

            Map<String, String> licenseMap = JSONObject.parseObject(content,Map.class);

            String dataJson = licenseMap.get("data");
            String signatureB64 = licenseMap.get("signature");

            if (!verifySignature(dataJson, signatureB64)) {
                throw new RuntimeException("Invalid license signature! File tampered.");
            }

            // ===== 第四步：解析内容并检查过期 =====
            Map<String, String> data = JSONObject.parseObject(dataJson, Map.class);
            String type = data.get("type");
            String expireStr = data.get("expire");
            String issueStr = data.get("issue");

            LocalDate expireDate = LocalDate.parse(expireStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate now = Instant.ofEpochMilli(currentTime).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate issueDate = LocalDate.parse(issueStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);

            // 新增：当前时间不得早于签发日（防止回拨到签发前）
            if (now.isBefore(issueDate)) {
                throw new RuntimeException("System time before license issue date! (Time tampering)");
            }

            // 永久版：只检查时间回拨，不过期
            if ("PERMANENT".equals(type)) {
                System.out.println(">>> PERMANENT license validated.");
                TimeTracker.updateLastValidTime(currentTime);
                return;
            }

            // 试用版：过期日期检查 + 30天硬限制
            if (now.isAfter(expireDate)) {
                throw new RuntimeException("License expired on " + expireStr + ". Please renew.");
            }
            if (issueStr != null) {
                long daysSinceIssue = ChronoUnit.DAYS.between(issueDate, now);
                if (daysSinceIssue > 30) {
                    throw new RuntimeException("Trial period (30 days) exceeded since issuance.");
                }
            }

            // ===== 第五步：全部通过，更新本次启动时间 =====
            TimeTracker.updateLastValidTime(currentTime);
            System.out.println(">>> TRIAL license validated. Expires: " + expireStr);

        } catch (Exception e) {
            System.err.println("LICENSE VALIDATION FAILED: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static boolean verifySignature(String dataJson, String signatureB64) throws Exception {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
        PublicKey publicKey = getPublicKeyFromString(LicenseConstants.PUBLIC_KEY);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(dataJson.getBytes("UTF-8"));
        return sig.verify(signatureBytes);
    }

    private static PublicKey getPublicKeyFromString(String keyStr) throws Exception {
        keyStr = keyStr.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}