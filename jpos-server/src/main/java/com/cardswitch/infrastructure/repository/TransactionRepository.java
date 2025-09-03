package com.cardswitch.infrastructure.repository;

import com.cardswitch.domain.transaction.Transaction;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository {
    void save(Transaction transaction);
    void update(Transaction transaction);
    Transaction findById(Long id);
    Transaction findByStan(String stan);
    List<Transaction> findByPanHashAndTimeRange(String panHash, LocalDateTime from, LocalDateTime to);
    List<Transaction> findPendingTransactions();
}