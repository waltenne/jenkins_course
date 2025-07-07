package com.example.demo.controllers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelloControllerTest {
    
    @Test
    void testDefaultName() {
        HelloController controller = new HelloController();
        String result = controller.determineName(null);
        assertEquals("World", result);
    }
    
    @Test
    void testCustomName() {
        HelloController controller = new HelloController();
        String result = controller.determineName("Jenkins");
        assertEquals("Jenkins", result);
    }
}