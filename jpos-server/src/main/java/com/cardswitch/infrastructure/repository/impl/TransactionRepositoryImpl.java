package com.cardswitch.infrastructure.repository.impl;

import com.cardswitch.domain.transaction.Transaction;
import com.cardswitch.infrastructure.repository.TransactionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public TransactionRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public void save(Transaction transaction) {
        String sql = """
            INSERT INTO transaction_log 
            (stan, mti, pan_hash, amount, currency_code, acquirer_id, terminal_id, 
             merchant_id, response_code, auth_code, transaction_time, settlement_date, status) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql,
            transaction.getStan(),
            transaction.getMti(),
            transaction.getPanHash(),
            transaction.getAmount(),
            transaction.getCurrencyCode(),
            transaction.getAcquirerId(),
            transaction.getTerminalId(),
            transaction.getMerchantId(),
            transaction.getResponseCode(),
            transaction.getAuthCode(),
            transaction.getTransactionTime(),
            transaction.getSettlementDate(),
            transaction.getStatus().name()
        );
    }
    
    @Override
    public void update(Transaction transaction) {
        String sql = """
            UPDATE transaction_log 
            SET response_code = ?, status = ?, auth_code = ? 
            WHERE id = ?
            """;
        
        jdbcTemplate.update(sql,
            transaction.getResponseCode(),
            transaction.getStatus().name(),
            transaction.getAuthCode(),
            transaction.getId()
        );
    }
    
    @Override
    public Transaction findById(Long id) {
        String sql = "SELECT * FROM transaction_log WHERE id = ?";
        List<Transaction> results = jdbcTemplate.query(sql, new TransactionRowMapper(), id);
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public Transaction findByStan(String stan) {
        String sql = "SELECT * FROM transaction_log WHERE stan = ? ORDER BY created_at DESC LIMIT 1";
        List<Transaction> results = jdbcTemplate.query(sql, new TransactionRowMapper(), stan);
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public List<Transaction> findByPanHashAndTimeRange(String panHash, LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT * FROM transaction_log 
            WHERE pan_hash = ? AND transaction_time BETWEEN ? AND ?
            ORDER BY transaction_time DESC
            """;
        return jdbcTemplate.query(sql, new TransactionRowMapper(), panHash, from, to);
    }
    
    @Override
    public List<Transaction> findPendingTransactions() {
        String sql = "SELECT * FROM transaction_log WHERE status = 'PENDING'";
        return jdbcTemplate.query(sql, new TransactionRowMapper());
    }
    
    private static class TransactionRowMapper implements RowMapper<Transaction> {
        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transaction txn = new Transaction();
            txn.setId(rs.getLong("id"));
            txn.setStan(rs.getString("stan"));
            txn.setMti(rs.getString("mti"));
            txn.setPanHash(rs.getString("pan_hash"));
            txn.setAmount(rs.getBigDecimal("amount"));
            // Set other fields...
            return txn;
        }
    }
}