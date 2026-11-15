package com.tdtu.soa.account.repository;

import com.tdtu.soa.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserId(Long userId);
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE)
     * Ngăn chặn hoàn toàn race condition khi nhiều giao dịch cùng trừ tiền 1 tài khoản
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    /**
     * Cập nhật số dư nguyên tử có điều kiện (Atomic Conditional Update)
     * Trả về số dòng cập nhật thành công (1 nếu đủ tiền, 0 nếu không đủ tiền)
     */
    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance - :amount, a.updatedAt = CURRENT_TIMESTAMP WHERE a.accountNumber = :accountNumber AND a.balance >= :amount")
    int atomicDeductBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}

