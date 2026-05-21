package com.dynamicmock.adapter.out.template;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class providing random data generation functions
 * for use in response templates (e.g., {{$randomInt}}, {{$randomBool}})
 */
public class RandomDataFunctions {
    
    private static final Random RANDOM = new Random();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String EMAIL_DOMAINS = "example.com,test.com,mock.com,local.dev";
    
    /**
     * Generate a random integer between min (inclusive) and max (inclusive)
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    /**
     * Generate a random integer between 0 and max (inclusive)
     */
    public static int randomInt(int max) {
        return randomInt(0, max);
    }
    
    /**
     * Generate a random boolean value
     */
    public static boolean randomBool() {
        return RANDOM.nextBoolean();
    }
    
    /**
     * Generate a random string of specified length
     */
    public static String randomString(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
    
    /**
     * Generate a random string of length 10
     */
    public static String randomString() {
        return randomString(10);
    }
    
    /**
     * Get current timestamp in milliseconds
     */
    public static long timestamp() {
        return Instant.now().toEpochMilli();
    }
    
    /**
     * Get current timestamp in seconds
     */
    public static long timestampSeconds() {
        return Instant.now().getEpochSecond();
    }
    
    /**
     * Generate a random UUID
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a random email address
     */
    public static String randomEmail() {
        String[] domains = EMAIL_DOMAINS.split(",");
        String domain = domains[RANDOM.nextInt(domains.length)];
        String username = randomString(8).toLowerCase();
        return username + "@" + domain;
    }
    
    /**
     * Get a random item from an array
     */
    @SuppressWarnings("unchecked")
    public static Object randomArrayItem(Object array) {
        if (array == null) {
            return null;
        }
        if (array instanceof List) {
            List<?> list = (List<?>) array;
            if (list.isEmpty()) {
                return null;
            }
            return list.get(RANDOM.nextInt(list.size()));
        }
        if (array instanceof Object[]) {
            Object[] arr = (Object[]) array;
            if (arr.length == 0) {
                return null;
            }
            return arr[RANDOM.nextInt(arr.length)];
        }
        throw new IllegalArgumentException("Expected List or Array, got: " + array.getClass());
    }
    
    /**
     * Generate a random object with common fields
     */
    public static Map<String, Object> randomObject() {
        return Map.of(
            "id", randomUUID(),
            "name", randomString(8),
            "value", randomInt(1, 100),
            "active", randomBool(),
            "timestamp", timestamp()
        );
    }
    
    /**
     * Generate a random number between 0.0 and 1.0
     */
    public static double randomFloat() {
        return RANDOM.nextDouble();
    }
    
    /**
     * Generate a random number between min and max (inclusive)
     */
    public static double randomFloat(double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        return min + (max - min) * RANDOM.nextDouble();
    }
}

