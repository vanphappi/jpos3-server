package com.example;

import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;

public class QueueingListener implements ISORequestListener {
    private final Space<String, Object> sp = SpaceFactory.getSpace("transient:default");
    private static final String QUEUE = "txnqueue"; // trùng với txnmgr

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        Context ctx = new Context();
        ctx.put("SOURCE", source);     // giữ nguyên ISOSource để gửi trả sau
        ctx.put("REQUEST", request);   // bản tin vào
        sp.out(QUEUE, ctx);            // đẩy vào hàng đợi cho TransactionManager
        return true;                   // nhận ok
    }
}
