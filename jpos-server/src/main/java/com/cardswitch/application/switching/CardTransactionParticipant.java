package com.cardswitch.application.switching;

import com.cardswitch.domain.transaction.Transaction;
import com.cardswitch.domain.transaction.TransactionStatus;
import com.cardswitch.application.service.TransactionService;
import com.cardswitch.application.service.RoutingService;
import com.cardswitch.application.service.FraudService;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Log;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HexFormat;

public class CardTransactionParticipant implements TransactionParticipant, LogSource {
    private Logger logger;
    private String realm = "CardTransactionParticipant";
    private Log log;

    private TransactionService transactionService;
    private RoutingService routingService;
    private FraudService fraudService;

    @Override
    public void setLogger(Logger logger, String realm) {
        this.logger = logger;
        this.realm = realm != null ? realm : this.realm;
        this.log = new Log(this.logger, this.realm);
    }

    @Override
    public Logger getLogger() { return logger; }

    @Override
    public String getRealm() { return realm; }

    @Override
    public int prepare(long id, Serializable ctxObj) {
        final Context ctx = (Context) ctxObj;

        try {
            ISOMsg request = (ISOMsg) ctx.get("REQUEST");
            if (request == null) {
                log.error("No request message found");
                return ABORTED | NO_JOIN;
            }

            // Create transaction object (đã bắt ISOException bên trong)
            Transaction transaction = createTransaction(request);
            ctx.put("TRANSACTION", transaction);

            // Log transaction
            transactionService.logTransaction(transaction);

            // Validate transaction
            ValidationResult validation = validateTransaction(request, transaction);
            if (!validation.isValid()) {
                log.warn("Transaction validation failed: " + validation.getError());
                ISOMsg response = createErrorResponse(request, validation.getResponseCode());
                ctx.put("RESPONSE", response);
                return PREPARED | NO_JOIN;
            }

            // Fraud screening
            if (fraudService.isSuspicious(transaction)) {
                log.warn("Transaction flagged as suspicious");
                ISOMsg response = createErrorResponse(request, "51"); // generic decline
                ctx.put("RESPONSE", response);
                return PREPARED | NO_JOIN;
            }

            // Route transaction
            String destination = routingService.route(transaction);
            if (destination == null) {
                log.error("No route found for transaction");
                ISOMsg response = createErrorResponse(request, "91"); // Switch inoperative
                ctx.put("RESPONSE", response);
                return PREPARED | NO_JOIN;
            }
            ctx.put("DESTINATION", destination);

            // Handle different transaction types (đã bắt ISOException bên trong)
            return handleTransactionType(request, transaction, ctx);

        } catch (Exception e) {
            log.error("Error processing transaction", e);
            return ABORTED | NO_JOIN;
        }
    }

    /** ĐÃ BỎ throws ISOException — tự bắt bên trong */
    private Transaction createTransaction(ISOMsg request) {
        String mti = "0000";
        try {
            mti = request.getMTI();
        } catch (ISOException e) {
            log.warn("getMTI failed in createTransaction, defaulting to 0000", e);
        }

        Transaction txn = new Transaction(mti, request.getString(11));

        if (request.hasField(2)) {
            txn.setPanHash(hashPan(request.getString(2)));
        }

        if (request.hasField(4)) {
            // amount in cents -> scale 2
            txn.setAmount(new BigDecimal(request.getString(4)).movePointLeft(2));
        }

        if (request.hasField(49)) {
            txn.setCurrencyCode(request.getString(49));
        }
        return txn;
    }

    private ValidationResult validateTransaction(ISOMsg request, Transaction transaction) {
        // Validate mandatory fields
        if (!request.hasField(2)) {
            return ValidationResult.invalid("30", "Format error - PAN missing");
        }
        if (!request.hasField(4) && !transaction.isNetworkManagement()) {
            return ValidationResult.invalid("30", "Format error - Amount missing");
        }
        if (!request.hasField(11)) {
            return ValidationResult.invalid("30", "Format error - STAN missing");
        }

        // Validate PAN
        String pan = request.getString(2);
        if (pan.length() < 13 || pan.length() > 19) {
            return ValidationResult.invalid("30", "Invalid PAN length");
        }

        // Validate amount
        if (request.hasField(4)) {
            try {
                BigDecimal amount = new BigDecimal(request.getString(4));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return ValidationResult.invalid("13", "Invalid amount");
                }
            } catch (NumberFormatException e) {
                return ValidationResult.invalid("30", "Invalid amount format");
            }
        }

        return ValidationResult.valid();
    }

    /** BẮT ISOException ngay trong hàm */
    private int handleTransactionType(ISOMsg request, Transaction transaction, Context ctx) {
        String mti;
        try {
            mti = request.getMTI();
        } catch (ISOException e) {
            log.warn("getMTI failed in handleTransactionType", e);
            ISOMsg response = createErrorResponse(request, "96"); // system malfunction
            ctx.put("RESPONSE", response);
            return PREPARED | NO_JOIN;
        }

        switch (mti) {
            case "0200": // Purchase
                return handlePurchase(request, transaction, ctx);
            case "0400": // Reversal
                return handleReversal(request, transaction, ctx);
            case "0800": // Network Management
                return handleNetworkManagement(request, transaction, ctx);
            default:
                log.warn("Unsupported MTI: " + mti);
                ISOMsg response = createErrorResponse(request, "40"); // Requested function not supported
                ctx.put("RESPONSE", response);
                return PREPARED | NO_JOIN;
        }
    }

    private int handlePurchase(ISOMsg request, Transaction transaction, Context ctx) {
        try {
            ISOMsg response = createApprovedResponse(request);
            ctx.put("RESPONSE", response);

            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setResponseCode("00");
            return PREPARED | NO_JOIN;
        } catch (Exception e) {
            log.error("handlePurchase failed", e);
            ISOMsg response = createErrorResponse(request, "96");
            ctx.put("RESPONSE", response);
            return PREPARED | NO_JOIN;
        }
    }

    private int handleReversal(ISOMsg request, Transaction transaction, Context ctx) {
        try {
            // Find original transaction
            String originalStan = request.getString(11);
            Transaction originalTxn = transactionService.findByStan(originalStan);

            if (originalTxn == null) {
                log.warn("Original transaction not found for reversal: " + originalStan);
                ISOMsg res = createErrorResponse(request, "25"); // Unable to locate record
                ctx.put("RESPONSE", res);
                return PREPARED | NO_JOIN;
            }

            // Process reversal
            ISOMsg response = createApprovedResponse(request);
            ctx.put("RESPONSE", response);

            // Update original transaction status
            originalTxn.setStatus(TransactionStatus.REVERSED);
            transactionService.updateTransaction(originalTxn);

            return PREPARED | NO_JOIN;
        } catch (Exception e) {
            log.error("handleReversal failed", e);
            ISOMsg response = createErrorResponse(request, "96");
            ctx.put("RESPONSE", response);
            return PREPARED | NO_JOIN;
        }
    }

    private int handleNetworkManagement(ISOMsg request, Transaction transaction, Context ctx) {
        // Echo test - simply respond with same fields
        ISOMsg response = (ISOMsg) request.clone();
        try {
            response.setMTI("0810");
        } catch (ISOException e) {
            log.warn("setMTI 0810 failed, keeping original MTI", e);
        }
        response.set(39, "00"); // Response code

        ctx.put("RESPONSE", response);
        return PREPARED | NO_JOIN;
    }

    /** ĐÃ BỎ throws ISOException — tự bắt và fallback */
    private ISOMsg createApprovedResponse(ISOMsg request) {
        ISOMsg response = (ISOMsg) request.clone();

        String reqMti = "0200";
        try {
            reqMti = request.getMTI();
        } catch (ISOException e) {
            log.warn("getMTI failed in createApprovedResponse, default to 0200", e);
        }

        String respMti;
        switch (reqMti) {
            case "0200": respMti = "0210"; break;
            case "0400": respMti = "0410"; break;
            case "0800": respMti = "0810"; break;
            default:     respMti = "0210"; break;
        }

        try {
            response.setMTI(respMti);
        } catch (ISOException e) {
            log.warn("setMTI failed in createApprovedResponse", e);
        }

        response.set(39, "00");                  // Approved
        response.set(38, generateAuthCode());    // Authorization code
        return response;
    }

    /** ĐÃ BỎ throws ISOException — không ném checked exception ra ngoài */
    private ISOMsg createErrorResponse(ISOMsg request, String responseCode) {
        ISOMsg response = createApprovedResponse(request);
        response.set(39, responseCode);
        response.unset(38); // Remove auth code for declines
        return response;
    }

    private String generateAuthCode() {
        return String.format("%06d", (int)(Math.random() * 1_000_000));
    }

    // ---- HEX utils (thay thế DatatypeConverter) ----
    private static String bytesToHexUpper(byte[] bytes) {
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    private String hashPan(String pan) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pan.getBytes());
            return bytesToHexUpper(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing PAN", e);
        }
    }

    @Override
    public void commit(long id, Serializable ctx) {
        if (log != null) log.info("Transaction " + id + " committed");
    }

    @Override
    public void abort(long id, Serializable ctx) {
        if (log != null) log.info("Transaction " + id + " aborted");
    }
}

// ValidationResult helper class (giữ nguyên)
class ValidationResult {
    private boolean valid;
    private String responseCode;
    private String error;

    private ValidationResult(boolean valid, String responseCode, String error) {
        this.valid = valid;
        this.responseCode = responseCode;
        this.error = error;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult invalid(String responseCode, String error) {
        return new ValidationResult(false, responseCode, error);
    }

    public boolean isValid() { return valid; }
    public String getResponseCode() { return responseCode; }
    public String getError() { return error; }
}
