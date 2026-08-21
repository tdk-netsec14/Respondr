package com.respondr.integration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class HmacUtils {

    private HmacUtils() {}

    /**
     * Computes HMAC SHA-256 signature for the raw body using the secret key.
     */
    public static String calculateHmacSha256(String body, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to calculate HMAC signature", e);
        }
    }

    /**
     * Verifies signature against payload using constant-time comparison to prevent timing attacks.
     */
    public static boolean verifyHmacSha256(String body, String secretKey, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }

        String cleanedSignature = providedSignature.trim();
        if (cleanedSignature.startsWith("sha256=")) {
            cleanedSignature = cleanedSignature.substring(7);
        }

        String expectedSignature = calculateHmacSha256(body, secretKey);
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = cleanedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }
}
