package com.cardswitch.application.service.impl;

import com.cardswitch.application.service.TransactionService;
import com.cardswitch.domain.transaction.Transaction;
import com.cardswitch.infrastructure.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
    
    private final TransactionRepository transactionRepository;
    
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @Override
    public void logTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }
    
    @Override
    public void updateTransaction(Transaction transaction) {
        transactionRepository.update(transaction);
    }
    
    @Override
    @Cacheable(value = "transactions", key = "#stan")
    public Transaction findByStan(String stan) {
        return transactionRepository.findByStan(stan);
    }
    
    @Override
    @Cacheable(value = "transactions", key = "#id")
    public Transaction findById(Long id) {
        return transactionRepository.findById(id);
    }
}