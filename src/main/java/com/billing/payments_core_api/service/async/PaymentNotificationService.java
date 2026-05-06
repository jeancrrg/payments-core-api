package com.billing.payments_core_api.service.async;

import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Side-effect work that runs after a payment/refund has been persisted.
 *
 * Methods here are invoked with @Async("paymentExecutor") so they run on the
 * dedicated thread pool defined in {@link com.billing.payments_core_api.config.AsyncConfig}.
 *
 * Typical work: dispatching webhooks, publishing events to a message broker,
 * sending email/SMS notifications, writing audit entries to a secondary store.
 * For this microservice the implementation is a logged simulation — replace
 * with the real integration when available.
 */
@Slf4j
@Service
public class PaymentNotificationService {

    @Async("paymentExecutor")
    public void notifyPaymentProcessed(Payment payment) {
        log.info("[ASYNC thread={}] Notifying payment processed: id={}, customer={}, status={}",
                Thread.currentThread().getName(), payment.getId(), payment.getCustomerId(), payment.getStatus());
        simulateExternalCall(150);
        log.info("[ASYNC] Notification dispatched for payment {}", payment.getId());
    }

    @Async("paymentExecutor")
    public void notifyRefundProcessed(Refund refund) {
        log.info("[ASYNC thread={}] Notifying refund processed: id={}, paymentId={}, status={}",
                Thread.currentThread().getName(), refund.getId(),
                refund.getPayment() != null ? refund.getPayment().getId() : null,
                refund.getStatus());
        simulateExternalCall(150);
        log.info("[ASYNC] Notification dispatched for refund {}", refund.getId());
    }

    @Async("paymentExecutor")
    public void auditPaymentEvent(Payment payment, String event) {
        log.info("[ASYNC AUDIT thread={}] event={}, paymentId={}, status={}",
                Thread.currentThread().getName(), event, payment.getId(), payment.getStatus());
    }

    private void simulateExternalCall(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
