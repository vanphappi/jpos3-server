package com.example;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.ISO87APackager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TestClient {
    public static void main(String[] args) throws Exception {
        ISOPackager packager = new ISO87APackager();

        NACChannel channel = new NACChannel();   // ctor rỗng (3.0.0)
        channel.setPackager(packager);
        channel.setHost("127.0.0.1");
        channel.setPort(9150);
        // channel.setTimeout(10000); // nếu bạn muốn, dùng biến kiểu NACChannel (không dùng ISOChannel)

        channel.connect();

        ISOMsg m = new ISOMsg();
        m.setMTI("0200");
        m.set(2,  "4111111111111111");
        m.set(3,  "000000");
        m.set(4,  "000000001000");
        m.set(7,  new SimpleDateFormat("MMddHHmmss").format(new Date())); // DE7 bắt buộc: 10 ký tự
        m.set(11, "000001");
        m.set(41, "12345678");
        m.set(42, "123456789012345");
        m.set(49, "840");

        channel.send(m);
        ISOMsg rsp = channel.receive();
        System.out.println("Got response MTI=" + rsp.getMTI() + ", DE39=" + rsp.getString(39));

        channel.disconnect();
    }
}
