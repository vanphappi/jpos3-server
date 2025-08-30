package com.company.jpos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class JposApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(JposApplication.class);
    
    public static void main(String[] args) {
        logger.info("🚀 Starting jPOS Spring Boot Application...");
        
        // Start Spring Boot context
        SpringApplication app = new SpringApplication(JposApplication.class);
        app.run(args);
        
        logger.info("✅ jPOS Spring Boot Application started!");
    }
}
