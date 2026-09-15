package com.payflux.common;

import java.security.SecureRandom;

public final class IdGenerator {
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {}

    public static String next(String prefix) {
        StringBuilder value = new StringBuilder(prefix);
        for (int i = 0; i < 20; i++)
            value.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return value.toString();
    }
}
