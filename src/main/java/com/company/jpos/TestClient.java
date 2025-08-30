package com.company.jpos;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.ISO87APackager;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestClient {
    
    public static void main(String[] args) {
        try {
            System.out.println("🧪 jPOS Test Client Starting...");
            
            Socket socket = new Socket("localhost", 8120);
            ASCIIChannel channel = new ASCIIChannel(socket, new ISO87APackager());
            channel.connect();
            System.out.println("✅ Connected to jPOS server");
            
            testNetworkTest(channel);
            Thread.sleep(1000);
            testAuthorization(channel, "4111111111111111", "000000001000");
            Thread.sleep(1000);
            testAuthorization(channel, "4111111111110001", "000000005000");
            
            channel.disconnect();
            System.out.println("👋 Test completed");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
        }
    }
    
    private static void testNetworkTest(ASCIIChannel channel) throws Exception {
        System.out.println("\n🔍 Testing Network Test (0800)...");
        
        ISOMsg request = new ISOMsg();
        request.setMTI("0800");
        request.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
        request.set(11, String.format("%06d", System.currentTimeMillis() % 1000000));
        
        channel.send(request);
        ISOMsg response = channel.receive();
        
        System.out.println("Response Code: " + response.getString(39));
    }
    
    private static void testAuthorization(ASCIIChannel channel, String pan, String amount) throws Exception {
        System.out.println(String.format("\n💳 Testing Authorization - PAN: %s", maskPAN(pan)));
        
        ISOMsg request = new ISOMsg();
        request.setMTI("0200");
        request.set(2, pan);
        request.set(3, "000000");
        request.set(4, amount);
        request.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
        request.set(11, String.format("%06d", System.currentTimeMillis() % 1000000));
        request.set(12, new SimpleDateFormat("HHmmss").format(new Date()));
        request.set(13, new SimpleDateFormat("MMdd").format(new Date()));
        request.set(41, "12345678");
        request.set(42, "123456789012345");
        
        channel.send(request);
        ISOMsg response = channel.receive();
        
        String code = response.getString(39);
        System.out.println("Response Code: " + code);
        
        switch (code) {
            case "00": System.out.println("Result: ✅ APPROVED"); break;
            case "51": System.out.println("Result: ❌ INSUFFICIENT FUNDS"); break;
            default: System.out.println("Result: ❌ DECLINED (" + code + ")"); break;
        }
    }
    
    private static String maskPAN(String pan) {
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
}
