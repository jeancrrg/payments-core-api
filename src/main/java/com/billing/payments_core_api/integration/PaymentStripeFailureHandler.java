package com.billing.payments_core_api.integration;

import com.billing.payments_core_api.exception.ExternalServiceException;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.repository.PaymentRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
import com.billing.payments_core_api.util.FormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStripeFailureHandler implements StripeFailureHandler<Payment> {

    private final PaymentRepository paymentRepository;
    private final PaymentNotificationService notificationService;
    private final FormatterUtil formatterUtil;

    @Override
    public void handle(Payment payment, ExternalServiceException e) {
        log.error("m=handle, paymentId={}, error={}", payment.getId(), e.getMessage());
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(formatterUtil.truncateMessage(e.getMessage()));
        paymentRepository.save(payment);
        notificationService.notifyPaymentProcessed(payment);
    }

}
