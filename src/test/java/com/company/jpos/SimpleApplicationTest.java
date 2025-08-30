package com.company.jpos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimpleApplicationTest {
    
    @Test
    void testBasicFunctionality() {
        String test = "Hello World";
        assertNotNull(test);
        assertEquals("Hello World", test);
    }
    
    @Test
    void testJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        assertNotNull(javaVersion);
        assertTrue(javaVersion.startsWith("23"));
    }
}
