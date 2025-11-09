package org.example.kursovoi_4_course_1.InnerClasses;

import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Логин по умолчанию читаемый — 6 символов, чередуются согласная/гласная.
 * Пароль по умолчанию длиной 10, содержит минимум 1 верх. регистр, 1 ниж.регистр, 1 цифру, 1 спец.знак.
 */
@NoArgsConstructor
public final class RandomUserGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String VOWELS = "aeiou";
    private static final String CONSONANTS = "bcdfghjklmnpqrstvwxyz";

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@?";

    public static String generateLogin() {
        return generateLogin(6);
    }

    public static String generateLogin(int length) {
        if (length < 2) throw new IllegalArgumentException("length must be >= 2");
        StringBuilder sb = new StringBuilder(length);

        boolean startWithConsonant = true;

        for (int i = 0; i < length; i++) {
            if ((i % 2 == 0) == startWithConsonant) {
                sb.append(CONSONANTS.charAt(RANDOM.nextInt(CONSONANTS.length())));
            } else {
                sb.append(VOWELS.charAt(RANDOM.nextInt(VOWELS.length())));
            }
        }

        if (RANDOM.nextInt(10) == 0) { // ~10% шанс
            sb.setCharAt(length - 1, Character.forDigit(RANDOM.nextInt(10), 10));
        }

        return sb.toString();
    }

    public static String generatePassword() {
        return generatePassword(10);
    }

    public static String generatePassword(int length) {
        if (length < 6) throw new IllegalArgumentException("Password length must be >= 6");

        List<Character> chars = new ArrayList<>(length);

        chars.add(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        chars.add(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        chars.add(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));

        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        for (int i = 4; i < length; i++) {
            chars.add(all.charAt(RANDOM.nextInt(all.length())));
        }

        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(length);
        for (char c : chars) sb.append(c);
        return sb.toString();
    }

    public static void main(String[] args) {
        String login = generateLogin();           // читаемый логин из 6 символов
        String password = generatePassword();     // читаемый, но сильный пароль

        System.out.println("Login: " + login);
        System.out.println("Password: " + password);
    }
}
