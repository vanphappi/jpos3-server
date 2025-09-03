package com.cardswitch.application.service;

import com.cardswitch.domain.transaction.Transaction;

public interface TransactionService {
    void logTransaction(Transaction transaction);
    void updateTransaction(Transaction transaction);
    Transaction findByStan(String stan);
    Transaction findById(Long id);
}
