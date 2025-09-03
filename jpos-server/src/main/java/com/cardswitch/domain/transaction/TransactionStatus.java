package com.cardswitch.domain.transaction;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    DECLINED,
    REVERSED,
    TIMEOUT,
    ERROR
}
