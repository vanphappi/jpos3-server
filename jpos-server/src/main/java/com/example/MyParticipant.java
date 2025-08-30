package com.example;

import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Log;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyParticipant implements TransactionParticipant, LogSource {
    private Logger logger;
    private String realm = "MyParticipant";
    private Log log;

    // ---- LogSource wiring (Q2 sẽ gọi các setter này) ----
    @Override
    public void setLogger(Logger logger, String realm) {
        this.logger = logger;
        this.realm  = realm != null ? realm : this.realm;
        this.log    = new Log(this.logger, this.realm);
    }
    @Override
    public Logger getLogger() { return logger; }
    @Override
    public String getRealm()  { return realm; }

    // ---- Business logic ----
    @Override
    public int prepare(long id, Serializable ctxObj) {
        final Context ctx = (Context) ctxObj;
        try {
            ISOMsg request = getRequestFrom(ctx);
            if (request == null) {
                if (log != null) log.error("No request message found in context");
                return ABORTED | NO_JOIN;
            }

            if (log != null) {
                log.info("Processing txn id=" + id + " MTI=" + request.getMTI());
                if (request.hasField(2))  log.info("PAN: " + maskPAN(request.getString(2)));
                if (request.hasField(4))  log.info("Amount: " + request.getString(4));
                if (request.hasField(11)) log.info("STAN: " + request.getString(11));
            }

            ISOMsg response = createResponse(request);
            ctx.put("RESPONSE", response);            // để ResponseParticipant lấy và gửi ra

            if (log != null) log.info("Txn " + id + " prepared");
            return PREPARED | NO_JOIN;

        } catch (Exception e) {
            if (log != null) log.error("Error processing txn " + id, e);
            return ABORTED | NO_JOIN;
        }
    }

    @Override
    public void commit(long id, Serializable ctx) {
        if (log != null) log.info("Txn " + id + " committed");
    }

    @Override
    public void abort(long id, Serializable ctx) {
        if (log != null) log.info("Txn " + id + " aborted");
    }

    private ISOMsg getRequestFrom(Context ctx) {
        Object v = ctx.get("REQUEST");
        if (v instanceof ISOMsg) return (ISOMsg) v;

        v = ctx.get(ISOMsg.class.getName()); // fallback thường gặp
        if (v instanceof ISOMsg) return (ISOMsg) v;

        v = ctx.get("request");               // fallback thêm
        if (v instanceof ISOMsg) return (ISOMsg) v;

        return null;
    }

    private ISOMsg createResponse(ISOMsg req) throws ISOException {
        ISOMsg resp = (ISOMsg) req.clone();

        // map MTI request -> response
        String mti = req.getMTI();
        resp.setMTI(
            "0200".equals(mti) ? "0210" :
            "0400".equals(mti) ? "0410" :
            "0800".equals(mti) ? "0810" : "0210"
        );

        // Response code
        resp.set(39, "00");

        // Transmission date & time
        if (!resp.hasField(7)) {
            String ts = new SimpleDateFormat("MMddHHmmss").format(new Date());
            resp.set(7, ts);
        }
        return resp;
    }

    private String maskPAN(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 6) + "****" + pan.substring(pan.length() - 4);
    }
}
