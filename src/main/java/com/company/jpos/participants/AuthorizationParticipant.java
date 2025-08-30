package com.company.jpos.participants;

import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class AuthorizationParticipant implements TransactionParticipant {
    
    private static final AtomicLong transactionCounter = new AtomicLong(0);
    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationParticipant.class);
    
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        long txnId = transactionCounter.incrementAndGet();
        
        try {
            ISOMsg request = (ISOMsg) ctx.get("REQUEST");
            if (request == null) {
                logError("No REQUEST found in context");
                return ABORTED;
            }
            
            String mti = request.getMTI();
            String stan = request.getString(11);
            String pan = maskPAN(request.getString(2));
            
            logInfo(String.format("[%d] Processing %s | STAN: %s | PAN: %s | Thread: %s", 
                   txnId, mti, stan, pan, 
                   Thread.currentThread().isVirtual() ? "Virtual" : "Platform"));
            
            ISOMsg response = processTransaction(request, txnId);
            
            if (response != null) {
                ctx.put("RESPONSE", response);
                ctx.put("TRANSACTION_ID", txnId);
                
                logInfo(String.format("[%d] Prepared successfully | Response: %s", 
                       txnId, response.getString(39)));
                return PREPARED;
            }
            
        } catch (Exception e) {
            logError("Transaction processing failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ABORTED;
    }
    
    @Override
    public void commit(long id, Serializable context) {
        Context ctx = (Context) context;
        Long txnId = (Long) ctx.get("TRANSACTION_ID");
        
        try {
            ISOMsg response = (ISOMsg) ctx.get("RESPONSE");
            ISOSource source = (ISOSource) ctx.get("SOURCE");
            
            if (response != null && source != null) {
                source.send(response);
                logInfo(String.format("[%d] Transaction committed | Response sent: %s", 
                       txnId != null ? txnId : id, response.getString(39)));
            }
        } catch (Exception e) {
            logError("Failed to send response: " + e.getMessage());
        }
    }
    
    @Override
    public void abort(long id, Serializable context) {
        logInfo(String.format("[%d] Transaction aborted", id));
    }
    
    private ISOMsg processTransaction(ISOMsg request, long txnId) throws Exception {
        String mti = request.getMTI();
        
        if (Thread.currentThread().isVirtual()) {
            Thread.sleep(10 + random.nextInt(40));
        }
        
        switch (mti) {
            case "0200":
                return processAuthorization(request, txnId);
            case "0800":
                return processNetworkTest(request, txnId);
            default:
                return processUnsupported(request, txnId);
        }
    }
    
    private ISOMsg processAuthorization(ISOMsg request, long txnId) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0210");
        response.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
        
        String pan = request.getString(2);
        String amount = request.getString(4);
        String responseCode = authorize(pan, amount, txnId);
        
        response.set(39, responseCode);
        
        if ("00".equals(responseCode)) {
            response.set(38, String.format("%06d", random.nextInt(1000000)));
        }
        
        return response;
    }
    
    private String authorize(String pan, String amount, long txnId) {
        try {
            if (pan == null || pan.length() < 13) return "14";
            if (amount == null || amount.trim().isEmpty()) return "13";
            
            long amountValue = Long.parseLong(amount);
            if (amountValue <= 0) return "13";
            if (amountValue > 10000000) return "61";
            if (pan.endsWith("0000")) return "14";
            
            String lastFour = pan.substring(Math.max(0, pan.length() - 4));
            if ("0001,0002,0003,0004,0005".contains(lastFour)) return "51";
            if (random.nextInt(100) == 0) return "96";
            
            return "00";
        } catch (Exception e) {
            return "96";
        }
    }
    
    private ISOMsg processNetworkTest(ISOMsg request, long txnId) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI("0810");
        response.set(39, "00");
        return response;
    }
    
    private ISOMsg processUnsupported(ISOMsg request, long txnId) throws Exception {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI(request.getMTI().substring(0, 2) + "10");
        response.set(39, "12");
        return response;
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
    
    private void logInfo(String message) {
        logger.info(message);
    }
    
    private void logError(String message) {
        logger.error(message);
    }
}