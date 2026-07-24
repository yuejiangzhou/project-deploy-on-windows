package org.unreal.model.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.Base64;
import java.util.Date;

public class TimeTracker {
    private static final String TRACK_FILE = "./.runtime/timestamp.dat";
    private static final String SECRET_KEY = "xuhuan2021__mms!"; // 可自定义，必须16字节
    private static final long ALLOWED_DRIFT_MS = 120000; // 允许1分钟时钟误差

    // ★ 读取：文件不存在直接抛出致命异常 ★
    public static long getLastValidTime() throws Exception {
        File file = new File(TRACK_FILE);
        if (!file.exists()) {
            throw new RuntimeException("FATAL: Timestamp file missing! Please do not delete .runtime folder. Contact support to restore.");
        }
        String encrypted = new String(Files.readAllBytes(file.toPath())).trim();
        String decrypted = decrypt(encrypted);
        return Long.parseLong(decrypted);
    }

    // ★ 写入：不创建目录，假定父目录已存在（由部署工具保证） ★
    public static void updateLastValidTime(long time) throws Exception {
        long lastValidTime = getLastValidTime();
        if (time - lastValidTime < 0) {
            throw new RuntimeException("FATAL: Timestamp file missing! Please do not delete .runtime folder. Contact support to restore.");
        }
        String encrypted = encrypt(String.valueOf(time));
        File file = new File(TRACK_FILE);
        // 若父目录不存在，写入会失败（但我们部署时会确保存在）
        Files.write(file.toPath(), encrypted.getBytes());
    }

    private static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SECRET_KEY.getBytes(), "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    private static String decrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SECRET_KEY.getBytes(), "AES"));
        return new String(cipher.doFinal(Base64.getDecoder().decode(data)));
    }


    public static String generateLastValidTime(Date date) throws Exception {

        return encrypt(String.valueOf(date.getTime()));
    }

    public static void main(String[] args) throws Exception{
        Date now = new Date(2026-1900,4,25);
        System.out.println(generateLastValidTime(now));
    }
}
