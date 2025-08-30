package com.company.jpos;

import org.jpos.q2.Q2;
import java.io.File;

public class JPOSServerMain {
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting jPOS 3.0.0 Server...");
        System.out.println("☕ Java Version: " + System.getProperty("java.version"));
        System.out.println("🧵 Virtual Threads: " + checkVirtualThreads());
        
        try {
            setupSystemProperties();
            createDirectories();
            startQ2Server();
            setupShutdownHook();
            
            System.out.println("✅ jPOS Server started successfully!");
            System.out.println("📄 Logs: tail -f log/q2.log");
            System.out.println("🔍 Check: netstat -an | grep 8120");
            System.out.println("⏹️  Press Ctrl+C to stop server");
            
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
                boolean created = directory.mkdirs();
                if (created) {
                    System.out.println("📁 Created directory: " + dir);
                }
            }
        }
    }
    
    private static void startQ2Server() throws Exception {
        Q2 q2 = new Q2();
        
        if (checkVirtualThreads()) {
            Thread.startVirtualThread(() -> {
                try {
                    System.out.println("🔄 Starting Q2 with Virtual Threads...");
                    q2.start();
                    System.out.println("✅ Q2 started successfully");
                } catch (Exception e) {
                    System.err.println("❌ Q2 start failed: " + e.getMessage());
                    System.exit(1);
                }
            });
        } else {
            new Thread(() -> {
                try {
                    System.out.println("🔄 Starting Q2...");
                    q2.start();
                    System.out.println("✅ Q2 started successfully");
                } catch (Exception e) {
                    System.err.println("❌ Q2 start failed: " + e.getMessage());
                    System.exit(1);
                }
            }).start();
        }
        
        Thread.sleep(3000);
    }
    
    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down...");
            System.out.println("👋 Goodbye!");
        }));
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
