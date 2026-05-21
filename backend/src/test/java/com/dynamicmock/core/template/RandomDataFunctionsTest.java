package com.dynamicmock.core.template;

import com.dynamicmock.adapter.out.template.RandomDataFunctions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RandomDataFunctionsTest {
    
    @RepeatedTest(10)
    void testRandomInt() {
        int value = RandomDataFunctions.randomInt(1, 100);
        assertTrue(value >= 1 && value <= 100);
    }
    
    @RepeatedTest(10)
    void testRandomIntSingleParam() {
        int value = RandomDataFunctions.randomInt(50);
        assertTrue(value >= 0 && value <= 50);
    }
    
    @Test
    void testRandomIntThrowsExceptionWhenMinGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> {
            RandomDataFunctions.randomInt(100, 1);
        });
    }
    
    @RepeatedTest(10)
    void testRandomBool() {
        boolean value = RandomDataFunctions.randomBool();
        assertNotNull(value);
    }
    
    @RepeatedTest(10)
    void testRandomString() {
        String value = RandomDataFunctions.randomString(10);
        assertNotNull(value);
        assertEquals(10, value.length());
    }
    
    @Test
    void testRandomStringEmpty() {
        String value = RandomDataFunctions.randomString(0);
        assertEquals("", value);
    }
    
    @Test
    void testRandomStringNegative() {
        String value = RandomDataFunctions.randomString(-1);
        assertEquals("", value);
    }
    
    @RepeatedTest(10)
    void testRandomStringDefault() {
        String value = RandomDataFunctions.randomString();
        assertNotNull(value);
        assertEquals(10, value.length());
    }
    
    @Test
    void testTimestamp() {
        long timestamp1 = RandomDataFunctions.timestamp();
        assertTrue(timestamp1 > 0);
        
        // Wait a bit
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long timestamp2 = RandomDataFunctions.timestamp();
        assertTrue(timestamp2 > timestamp1);
    }
    
    @Test
    void testTimestampSeconds() {
        long timestamp = RandomDataFunctions.timestampSeconds();
        assertTrue(timestamp > 0);
        // Should be roughly current time (within last minute)
        long currentSeconds = System.currentTimeMillis() / 1000;
        assertTrue(Math.abs(timestamp - currentSeconds) < 60);
    }
    
    @RepeatedTest(10)
    void testRandomUUID() {
        String uuid = RandomDataFunctions.randomUUID();
        assertNotNull(uuid);
        assertTrue(uuid.length() > 0);
        // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        assertTrue(uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }
    
    @RepeatedTest(10)
    void testRandomEmail() {
        String email = RandomDataFunctions.randomEmail();
        assertNotNull(email);
        assertTrue(email.contains("@"));
        assertTrue(email.contains("."));
    }
    
    @RepeatedTest(10)
    void testRandomArrayItem() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object item = RandomDataFunctions.randomArrayItem(list);
        assertNotNull(item);
        assertTrue(list.contains(item));
    }
    
    @Test
    void testRandomArrayItemEmptyList() {
        List<String> emptyList = Arrays.asList();
        Object item = RandomDataFunctions.randomArrayItem(emptyList);
        assertNull(item);
    }
    
    @Test
    void testRandomArrayItemNull() {
        Object item = RandomDataFunctions.randomArrayItem(null);
        assertNull(item);
    }
    
    @RepeatedTest(10)
    void testRandomArrayItemArray() {
        String[] array = {"x", "y", "z"};
        Object item = RandomDataFunctions.randomArrayItem(array);
        assertNotNull(item);
        assertTrue(Arrays.asList(array).contains(item));
    }
    
    @RepeatedTest(10)
    void testRandomObject() {
        Map<String, Object> obj = RandomDataFunctions.randomObject();
        assertNotNull(obj);
        assertTrue(obj.containsKey("id"));
        assertTrue(obj.containsKey("name"));
        assertTrue(obj.containsKey("value"));
        assertTrue(obj.containsKey("active"));
        assertTrue(obj.containsKey("timestamp"));
    }
    
    @RepeatedTest(10)
    void testRandomFloat() {
        double value = RandomDataFunctions.randomFloat();
        assertTrue(value >= 0.0 && value < 1.0);
    }
    
    @RepeatedTest(10)
    void testRandomFloatRange() {
        double value = RandomDataFunctions.randomFloat(10.0, 20.0);
        assertTrue(value >= 10.0 && value <= 20.0);
    }
    
    @Test
    void testRandomFloatThrowsExceptionWhenMinGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> {
            RandomDataFunctions.randomFloat(20.0, 10.0);
        });
    }
}

