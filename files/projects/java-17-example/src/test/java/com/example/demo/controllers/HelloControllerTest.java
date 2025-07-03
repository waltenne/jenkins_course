package com.example.demo.controllers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HelloControllerTest {
    
    @Test
    public void testDefaultName() {
        HelloController controller = new HelloController();
        String result = controller.determineName(null);
        assertEquals("World", result);
    }
    
    @Test
    public void testCustomName() {
        HelloController controller = new HelloController();
        String result = controller.determineName("Jenkins");
        assertEquals("Jenkins", result);
    }
    
    // Adicione este método à classe HelloController para os testes
    // public String determineName(String name) {
    //     return (name == null || name.trim().isEmpty()) ? "World" : name;
    // }
}