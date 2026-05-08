package com.billing.payments_core_api.service.async;

import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentNotificationService {

    @Async("paymentExecutor")
    public void notifyPaymentProcessed(Payment payment) {
        log.info("[ASYNC thread={}] Notifying payment processed: id={}, customer={}, status={}",
                Thread.currentThread().getName(), payment.getId(), payment.getCustomerId(), payment.getStatus());
        simulateExternalCall();
        log.info("[ASYNC] Notification dispatched for payment {}", payment.getId());
    }

    @Async("paymentExecutor")
    public void notifyRefundProcessed(Refund refund) {
        log.info("[ASYNC thread={}] Notifying refund processed: id={}, paymentId={}, status={}",
                Thread.currentThread().getName(), refund.getId(), refund.getPaymentId(), refund.getStatus());
        simulateExternalCall();
        log.info("[ASYNC] Notification dispatched for refund {}", refund.getId());
    }

    @Async("paymentExecutor")
    public void auditPaymentEvent(Payment payment, String event) {
        log.info("[ASYNC AUDIT thread={}] event={}, paymentId={}, status={}",
                Thread.currentThread().getName(), event, payment.getId(), payment.getStatus());
    }

    private void simulateExternalCall() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
