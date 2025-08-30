package com.company.jpos.handlers;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleTransactionHandler implements ISORequestListener {
    
    private final AtomicLong transactionCounter = new AtomicLong(0);
    private final AtomicLong successCounter = new AtomicLong(0);
    private final Random random = new Random();
    
    public SimpleTransactionHandler() {
        System.out.println("Transaction Handler initialized");
        System.out.println("Virtual Threads: " + supportsVirtualThreads());
    }
    
    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        long txnId = transactionCounter.incrementAndGet();
        
        if (supportsVirtualThreads()) {
            Thread.startVirtualThread(() -> handleTransaction(source, request, txnId));
        } else {
            new Thread(() -> handleTransaction(source, request, txnId)).start();
        }
        
        return true;
    }
    
    private void handleTransaction(ISOSource source, ISOMsg request, long txnId) {
        long startTime = System.currentTimeMillis();
        
        try {
            String mti = request.getMTI();
            String pan = request.getString(2);
            String amount = request.getString(4);
            String stan = request.getString(11);
            
            System.out.println(String.format(
                "[%d] %s | %s | %s | %s | %s", 
                txnId, mti, maskPAN(pan), amount, stan, 
                Thread.currentThread().isVirtual() ? "Virtual" : "Platform"
            ));
            
            ISOMsg response = processMessage(request, mti);
            
            if (response != null) {
                source.send(response);
                successCounter.incrementAndGet();
                
                long duration = System.currentTimeMillis() - startTime;
                System.out.println(String.format(
                    "[%d] Success | %s | %dms", 
                    txnId, response.getString(39), duration
                ));
            }
            
        } catch (Exception e) {
            System.out.println("[" + txnId + "] Error: " + e.getMessage());
        }
        
        if (txnId % 50 == 0) {
            printStats();
        }
    }
    
    private ISOMsg processMessage(ISOMsg request, String mti) throws Exception {
        switch (mti) {
            case "0200": return processAuthorization(request);
            case "0420": return processReversal(request);
            case "0800": return processNetworkTest(request);
            default: return processUnsupported(request);
        }
    }
    
    private ISOMsg processAuthorization(ISOMsg request) throws Exception {
        Thread.sleep(50 + random.nextInt(50));
        
        String pan = request.getString(2);
        String amount = request.getString(4);
        
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0210");
        response.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
        response.set(39, authorize(pan, amount));
        
        return response;
    }
    
    private String authorize(String pan, String amount) {
        try {
            if (pan == null || pan.length() < 13) return "14";
            
            long amountValue = Long.parseLong(amount);
            if (amountValue <= 0) return "13";
            if (amountValue > 10000000) return "61";
            if (pan.endsWith("0000")) return "14";
            
            String lastFour = pan.substring(Math.max(0, pan.length() - 4));
            if ("0001,0002,0003,0004,0005".contains(lastFour)) return "51";
            if (random.nextInt(100) == 0) return "96";
            
            return "00";
        } catch (Exception e) {
            return "30";
        }
    }
    
    private ISOMsg processReversal(ISOMsg request) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0430");
        response.set(39, "00");
        return response;
    }
    
    private ISOMsg processNetworkTest(ISOMsg request) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0810");
        response.set(39, "00");
        return response;
    }
    
    private ISOMsg processUnsupported(ISOMsg request) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI(request.getMTI().substring(0, 2) + "10");
        response.set(39, "12");
        return response;
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) return pan;
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
    
    private boolean supportsVirtualThreads() {
        try {
            Thread.class.getMethod("startVirtualThread", Runnable.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    private void printStats() {
        long total = transactionCounter.get();
        long success = successCounter.get();
        double rate = total > 0 ? (double) success / total * 100 : 0;
        System.out.println(String.format(
            "Stats | Total: %d | Success: %d | Rate: %.1f%%", 
            total, success, rate
        ));
    }
}