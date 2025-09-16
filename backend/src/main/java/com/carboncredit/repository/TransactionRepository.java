package com.carboncredit.repository;

import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    List<Transaction> findByBuyer(User buyer);
    
    List<Transaction> findBySeller(User seller);
    
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    List<Transaction> findByBuyerAndStatus(User buyer, Transaction.TransactionStatus status);
    
    List<Transaction> findBySellerAndStatus(User seller, Transaction.TransactionStatus status);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.seller = :seller AND t.status = 'COMPLETED'")
    BigDecimal getTotalEarningsBySeller(User seller);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.buyer = :buyer AND t.status = 'COMPLETED'")
    BigDecimal getTotalSpendingsByBuyer(User buyer);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsBetween(LocalDateTime start, LocalDateTime end);
}
