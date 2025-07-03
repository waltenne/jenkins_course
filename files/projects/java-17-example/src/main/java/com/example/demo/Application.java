package com.example.demo;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class Application implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Aplicação iniciada - Versão 1.0");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Aplicação encerrada");
    }
}