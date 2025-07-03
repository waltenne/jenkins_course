package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HelloControllerIT {
    
    @Test
    public void testDoGet() throws Exception {
        // Mock dos objetos de requisição e resposta
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        // Configurar o mock para retornar um parâmetro "name"
        when(request.getParameter("name")).thenReturn("Jenkins");
        
        // Capturar a saída
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // Executar o método
        HelloController controller = new HelloController();
        controller.doGet(request, response);
        
        // Verificar resultados
        writer.flush();
        assertTrue(stringWriter.toString().contains("Hello, Jenkins!"));
    }
}