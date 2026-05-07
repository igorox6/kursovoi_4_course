package org.example.kursovoi_4_course_1.InnerClasses;

import java.util.Base64;

public final class Crypt {

    private static final String KEY = "simple_xor_key";

    private Crypt() {
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        byte[] keyBytes = KEY.getBytes();
        byte[] dataBytes = plainText.getBytes();
        byte[] result = new byte[dataBytes.length];

        for (int i = 0; i < dataBytes.length; i++) {
            result[i] = (byte) (dataBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return Base64.getEncoder().encodeToString(result);
    }

    public static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return "";
        }
        try {
            byte[] keyBytes = KEY.getBytes();
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            byte[] result = new byte[encryptedBytes.length];

            for (int i = 0; i < encryptedBytes.length; i++) {
                result[i] = (byte) (encryptedBytes[i] ^ keyBytes[i % keyBytes.length]);
            }

            return new String(result);
        } catch (Exception e) {
            System.err.println("Decrypt failed: " + e.getMessage());
            return "";
        }
    }
}
