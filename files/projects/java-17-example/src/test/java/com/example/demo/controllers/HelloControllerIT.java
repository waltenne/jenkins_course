package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class HelloControllerIT {
    
    @Test
    public void testDoGet() throws Exception {
        // Cria mocks
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        // Configura comportamento
        when(request.getParameter("name")).thenReturn("Jenkins");
        
        // Captura a saída
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // Executa o teste
        com.example.demo.controllers.HelloController controller = 
            new com.example.demo.controllers.HelloController();
        controller.doGet(request, response);
        
        // Verifica resultados
        writer.flush();
        assertTrue(stringWriter.toString().contains("Hello, Jenkins!"));
    }
}