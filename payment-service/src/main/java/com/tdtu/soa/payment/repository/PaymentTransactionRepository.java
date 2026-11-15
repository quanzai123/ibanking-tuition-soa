package com.tdtu.soa.payment.repository;

import com.tdtu.soa.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}

