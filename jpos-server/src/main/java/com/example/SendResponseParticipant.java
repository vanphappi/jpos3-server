package com.example;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.iso.ISOException;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

import java.io.IOException;
import java.io.Serializable;

public class SendResponseParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable ctxObj) {
        Context ctx = (Context) ctxObj;
        ISOSource src = (ISOSource) ctx.get("SOURCE");
        ISOMsg rsp = (ISOMsg) ctx.get("RESPONSE");

        if (src == null || rsp == null) {
            // thiếu source hoặc response -> không gửi được
            return ABORTED | NO_JOIN;
        }

        try {
            src.send(rsp);                 // gửi trả về cùng session
            return PREPARED | NO_JOIN;
        } catch (ISOException | IOException e) {
            e.printStackTrace();
            return ABORTED | NO_JOIN;
        }
    }

    @Override public void commit(long id, Serializable context) { /* no-op */ }
    @Override public void abort(long id, Serializable context)  { /* no-op */ }
}
