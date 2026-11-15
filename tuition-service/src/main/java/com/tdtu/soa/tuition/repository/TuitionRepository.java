package com.tdtu.soa.tuition.repository;

import com.tdtu.soa.tuition.entity.Tuition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TuitionRepository extends JpaRepository<Tuition, Long> {
    Optional<Tuition> findByMssv(String mssv);

    /**
     * ATOMIC DEBT CLEARING VỚI ĐIỀU KIỆN TRẠNG THÁI (Concurrency Protection)
     * Trả về:
     * - 1 nếu gạch nợ thành công (chuyển từ UNPAID -> PAID)
     * - 0 nếu khoản nợ đã bị ai đó thanh toán trước (status != UNPAID)
     */
    @Modifying
    @Query("UPDATE Tuition t SET t.status = 'PAID', t.paidAt = CURRENT_TIMESTAMP, t.transactionRef = :transactionRef WHERE t.mssv = :mssv AND t.status = 'UNPAID'")
    int atomicClearDebt(@Param("mssv") String mssv, @Param("transactionRef") String transactionRef);
}

