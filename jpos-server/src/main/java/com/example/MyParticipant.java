package com.example;

import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main transaction participant for processing ISO messages
 */
public class MyParticipant implements TransactionParticipant {
    private Log log;
    
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        
        try {
            ISOMsg request = (ISOMsg) ctx.get("REQUEST");
            
            if (request == null) {
                log.error("No request message found in context");
                return ABORTED | NO_JOIN;
            }
            
            String mti = request.getMTI();
            log.info("Processing transaction " + id + " with MTI: " + mti);
            
            // Log key fields
            if (request.hasField(2)) {
                log.info("PAN: " + maskPAN(request.getString(2)));
            }
            if (request.hasField(4)) {
                log.info("Amount: " + request.getString(4));
            }
            if (request.hasField(11)) {
                log.info("STAN: " + request.getString(11));
            }
            
            // Create response based on message type
            ISOMsg response = createResponse(request);
            ctx.put("RESPONSE", response);
            
            log.info("Transaction " + id + " prepared successfully");
            return PREPARED | NO_JOIN;
            
        } catch (Exception e) {
            log.error("Error processing transaction " + id, e);
            return ABORTED | NO_JOIN;
        }
    }
    
    @Override
    public void commit(long id, Serializable context) {
        log.info("Transaction " + id + " committed successfully");
    }
    
    @Override
    public void abort(long id, Serializable context) {
        log.info("Transaction " + id + " aborted");
    }
    
    private ISOMsg createResponse(ISOMsg request) throws ISOException {
        ISOMsg response = (ISOMsg) request.clone();
        
        // Set response MTI
        String requestMTI = request.getMTI();
        String responseMTI = getResponseMTI(requestMTI);
        response.setMTI(responseMTI);
        
        // Set response code
        response.set(39, "00"); // Approved
        
        // Set transmission date/time if not present
        if (!response.hasField(7)) {
            String timestamp = new SimpleDateFormat("MMddHHmmss").format(new Date());
            response.set(7, timestamp);
        }
        
        return response;
    }
    
    private String getResponseMTI(String requestMTI) {
        // Convert request MTI to response MTI
        switch (requestMTI) {
            case "0200": return "0210"; // Authorization request -> response
            case "0400": return "0410"; // Reversal request -> response
            case "0800": return "0810"; // Network management request -> response
            default: return "0210";     // Default response
        }
    }
    
    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) {
            return "****";
        }
        return pan.substring(0, 6) + "****" + pan.substring(pan.length() - 4);
    }
    
    public void setLog(Log log) {
        this.log = log;
    }
}
