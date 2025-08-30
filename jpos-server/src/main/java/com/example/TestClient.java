package com.example;

import org.jpos.iso.*;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.ISO87APackager;

public class TestClient {
    public static void main(String[] args) {
        System.out.println("jPOS Test Client");
        System.out.println("Connecting to localhost:9150");

        NACChannel channel = null;
        try {
            // 1) Packager khớp server
            ISOPackager packager = new ISO87APackager();

            // 2) Channel khớp header/cổng
            channel = new NACChannel();
            channel.setHost("localhost");
            channel.setPort(9150);
            channel.setPackager(packager);
            channel.setHeader("ISO015000077");   // <<< header phải giống server
            channel.setTimeout(30000);

            // 3) Connect
            channel.connect();
            System.out.println("Connected successfully");

            // 4) Tạo message test
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);          // giúp log/trace đẹp
            msg.setMTI("0200");
            msg.set(2, "4111111111111111");     // PAN (LLVAR)
            msg.set(3, "000000");               // Processing code
            msg.set(4, "000000001000");         // Amount
            msg.set(11, "000001");              // STAN
            msg.set(41, "12345678");            // Terminal ID (8 là chuẩn)
            msg.set(42, "123456789012345");     // Merchant ID
            msg.set(49, "840");                 // Currency (USD)

            System.out.println("Sending message:");
            msg.dump(System.out, ">> ");

            // 5) Send/receive
            channel.send(msg);
            ISOMsg response = channel.receive();

            System.out.println("Received response:");
            response.dump(System.out, "<< ");
            System.out.println("Response code (39): " + response.getString(39));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null && channel.isConnected()) {
                    channel.disconnect();
                    System.out.println("Disconnected");
                }
            } catch (Exception ignore) {}
        }
    }
}
