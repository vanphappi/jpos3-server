package com.cardswitch.application.service;

import com.cardswitch.domain.transaction.Transaction;

public interface FraudService {
    boolean isSuspicious(Transaction transaction);
}