package com.example;

import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.iso.ISOMsg;
import org.jpos.util.Log;

import java.io.Serializable;

/**
 * Participant for sending responses back to clients
 */
public class ResponseParticipant implements TransactionParticipant {
    private Log log;
    
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        
        try {
            ISOMsg response = (ISOMsg) ctx.get("RESPONSE");
            
            if (response != null) {
                log.info("Sending response for transaction " + id + ": " + response.getMTI());
                // Response will be automatically sent by the channel adaptor
                return PREPARED | NO_JOIN;
            } else {
                log.warn("No response to send for transaction " + id);
                return ABORTED | NO_JOIN;
            }
            
        } catch (Exception e) {
            log.error("Error in response participant for transaction " + id, e);
            return ABORTED | NO_JOIN;
        }
    }
    
    @Override
    public void commit(long id, Serializable context) {
        log.debug("Response sent successfully for transaction " + id);
    }
    
    @Override
    public void abort(long id, Serializable context) {
        log.warn("Response sending aborted for transaction " + id);
    }
    
    public void setLog(Log log) {
        this.log = log;
    }
}
