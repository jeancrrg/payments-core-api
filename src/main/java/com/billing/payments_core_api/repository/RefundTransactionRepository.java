package com.billing.payments_core_api.repository;

import com.billing.payments_core_api.model.entity.RefundTransaction;
import com.billing.payments_core_api.model.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {

    List<RefundTransaction> findByPaymentId(UUID paymentId);

    @Query("""
        SELECT COALESCE(SUM(rfd.amount), 0)
          FROM RefundTransaction rfd
         WHERE 1=1
           AND rfd.paymentId = :paymentId
           AND rfd.status <> :status
    """)
    BigDecimal sumRefundedAmountForPayment(@Param("paymentId") UUID paymentId, @Param("status") RefundStatus status);

}
