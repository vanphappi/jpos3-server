package com.example;

import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Log;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.iso.ISOMsg;

import java.io.Serializable;

public class ResponseParticipant implements TransactionParticipant, LogSource {
    private Logger logger; private String realm="ResponseParticipant"; private Log log;
    private final Space<Object,Object> sp = SpaceFactory.getSpace();

    @Override public void setLogger(Logger logger, String realm) {
        this.logger = logger; this.realm = realm!=null?realm:this.realm; this.log=new Log(this.logger,this.realm);
    }
    @Override public Logger getLogger(){return logger;}
    @Override public String getRealm(){return realm;}

    @Override
    public int prepare(long id, Serializable ctxObj) {
        Context ctx = (Context) ctxObj;
        ISOMsg resp = (ISOMsg) ctx.get("RESPONSE");
        if (resp == null) {
            if (log != null) log.warn("No RESPONSE in context; skipping send");
            return PREPARED | NO_JOIN | READONLY;
        }
        // đẩy ra queue 'out' ==> ChannelAdaptor gửi ra socket
        sp.out("out", resp);
        if (log != null) log.info("Response enqueued to 'out'");
        return PREPARED | NO_JOIN;
    }
    @Override public void commit(long id, Serializable ctx) { }
    @Override public void abort(long id, Serializable ctx) { }
}
