package com.example;

import org.jpos.iso.*;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;

/**
 * Simple test client for testing the jPOS server
 */
public class TestClient {
    public static void main(String[] args) throws Exception {
        System.out.println("jPOS Test Client");
        System.out.println("Connecting to localhost:8000");
        
        // Create packager programmatically to avoid file path issues
        ISOPackager packager = createPackager();
        
        // Create channel - correct way for jPOS 3.0
        NACChannel channel = new NACChannel();
        channel.setHost("localhost");
        channel.setPort(8000);
        channel.setPackager(packager);
        
        try {
            // Connect to server
            channel.connect();
            System.out.println("Connected successfully");
            
            // Create test message
            ISOMsg msg = new ISOMsg();
            msg.setMTI("0200");
            msg.set(2, "4111111111111111");    // PAN
            msg.set(3, "000000");              // Processing code
            msg.set(4, "000000001000");        // Amount
            msg.set(11, "000001");             // STAN
            msg.set(41, "12345678");           // Terminal ID
            msg.set(42, "123456789012345");    // Merchant ID
            msg.set(49, "840");                // Currency (USD)
            
            System.out.println("Sending message: " + msg);
            
            // Send and receive
            channel.send(msg);
            ISOMsg response = channel.receive();
            
            System.out.println("Received response: " + response);
            System.out.println("Response code: " + response.getString(39));
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Disconnect
            if (channel.isConnected()) {
                channel.disconnect();
                System.out.println("Disconnected");
            }
        }
    }
    
    private static ISOPackager createPackager() throws ISOException {
        // Create a simple packager programmatically
        GenericPackager packager = new GenericPackager();
        
        // Set packager configuration programmatically
        ISOFieldPackager[] packagers = new ISOFieldPackager[128];
        
        // MTI
        packagers[0] = new IFA_NUMERIC(4, "MESSAGE TYPE INDICATOR");
        
        // Primary bitmap
        packagers[1] = new IFA_BITMAP(16, "BIT MAP");
        
        // Common fields
        packagers[2] = new IFA_LLNUM(19, "PAN - PRIMARY ACCOUNT NUMBER");
        packagers[3] = new IFA_NUMERIC(6, "PROCESSING CODE");
        packagers[4] = new IFA_NUMERIC(12, "AMOUNT, TRANSACTION");
        packagers[7] = new IFA_NUMERIC(10, "TRANSMISSION DATE AND TIME");
        packagers[11] = new IFA_NUMERIC(6, "SYSTEM TRACE AUDIT NUMBER");
        packagers[12] = new IFA_NUMERIC(6, "TIME, LOCAL TRANSACTION");
        packagers[13] = new IFA_NUMERIC(4, "DATE, LOCAL TRANSACTION");
        packagers[15] = new IFA_NUMERIC(4, "DATE, SETTLEMENT");
        packagers[37] = new IF_CHAR(12, "RETRIEVAL REFERENCE NUMBER");
        packagers[39] = new IF_CHAR(2, "RESPONSE CODE");
        packagers[41] = new IF_CHAR(16, "CARD ACCEPTOR TERMINAL IDENTIFICATION");
        packagers[42] = new IF_CHAR(15, "CARD ACCEPTOR IDENTIFICATION CODE");
        packagers[49] = new IFA_NUMERIC(3, "CURRENCY CODE, TRANSACTION");
        packagers[102] = new IFA_LLNUM(28, "ACCOUNT IDENTIFICATION 1");
        packagers[103] = new IFA_LLNUM(28, "ACCOUNT IDENTIFICATION 2");
        
        packager.setFieldPackager(packagers);
        return packager;
    }
}
