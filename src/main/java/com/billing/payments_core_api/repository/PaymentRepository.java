package com.billing.payments_core_api.repository;

import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByCustomerId(String customerId, Pageable pageable);

    Page<Payment> findByCustomerIdAndStatus(String customerId, PaymentStatus status, Pageable pageable);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    boolean existsByStripePaymentIntentId(String stripePaymentIntentId);
}
