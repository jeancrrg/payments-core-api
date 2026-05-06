package com.billing.payments_core_api.repository;

import com.billing.payments_core_api.model.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByPaymentId(UUID paymentId);

    Optional<Refund> findByStripeRefundId(String stripeRefundId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(r.amount), 0) FROM Refund r " +
            "WHERE r.payment.id = :paymentId AND r.status <> com.billing.payments_core_api.model.entity.RefundStatus.FAILED"
    )
    BigDecimal sumRefundedAmountForPayment(@org.springframework.data.repository.query.Param("paymentId") UUID paymentId);
}
