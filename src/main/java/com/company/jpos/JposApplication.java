package com.company.jpos;

import org.jpos.q2.Q2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import jakarta.annotation.PreDestroy;
import java.io.File;

@SpringBootApplication
public class JposApplication implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(JposApplication.class);
    private Q2 q2;

    public static void main(String[] args) {
        System.out.println("🚀 Starting jPOS 3.0.0 Server...");
        System.out.println("☕ Java Version: " + System.getProperty("java.version"));

        // Set system properties before Spring application starts
        setupSystemProperties();
        createDirectories();

        SpringApplication.run(JposApplication.class, args);
    }

    /**
     * This method is triggered by Spring after the application context has been
     * completely initialized. This is the ideal and safe point to initialize and start
     * third-party systems like jPOS Q2, as the entire class loading environment
     * is stable and all Spring beans are available.
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("Spring Boot context refreshed. Starting jPOS Q2 server...");
        startQ2Server();
    }

    /**
     * This method is triggered by Spring during the application shutdown process.
     * It ensures that the Q2 server is stopped gracefully.
     */
    @PreDestroy
    public void stopQ2Server() {
        if (q2 != null) {
            logger.info("Spring Boot is shutting down. Stopping jPOS Q2 server...");
            q2.shutdown();
            logger.info("jPOS Q2 server stopped.");
        }
    }

    private void startQ2Server() {
        Runnable startQ2 = () -> {
            // Set the context class loader for this new thread to ensure it inherits
            // the correct environment from the main Spring Boot application.
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            try {
                // Instantiate and start Q2 within this correctly configured thread.
                q2 = new Q2();
                q2.start();
                
                logger.info("✅ jPOS + Spring Boot started successfully!");
                logger.info("📄 Logs: tail -f log/q2.log");
                logger.info("🌐 Health check: curl http://localhost:8080/actuator/health");
                logger.info("📊 Metrics: curl http://localhost:8080/actuator/prometheus");
                logger.info("⏹️  Press Ctrl+C to stop server");

            } catch (Exception e) {
                logger.error("❌ Q2 start failed", e);
            }
        };

        if (checkVirtualThreads()) {
            logger.info("🔄 Starting Q2 with Virtual Threads...");
            Thread.startVirtualThread(startQ2);
        } else {
            logger.info("🔄 Starting Q2 with Platform Threads...");
            new Thread(startQ2).start();
        }
    }

    private static void setupSystemProperties() {
        System.setProperty("jpos.config.dir", "cfg");
        System.setProperty("jpos.deploy.dir", "deploy");
        System.setProperty("jpos.log.dir", "log");
        System.setProperty("jpos.data.dir", "data");
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        System.out.println("⚙️  System properties configured");
    }

    private static void createDirectories() {
        String[] dirs = {"cfg", "deploy", "log", "data"};
        for (String dir : dirs) {
            File directory = new File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("📁 Created directory: " + dir);
                }
            }
        }
    }

    private static boolean checkVirtualThreads() {
        try {
            Thread.class.getMethod("startVirtualThread", Runnable.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}

