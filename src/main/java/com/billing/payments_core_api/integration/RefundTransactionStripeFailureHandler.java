package com.billing.payments_core_api.integration;

import com.billing.payments_core_api.exception.ExternalServiceException;
import com.billing.payments_core_api.model.entity.RefundTransaction;
import com.billing.payments_core_api.model.enums.RefundStatus;
import com.billing.payments_core_api.repository.RefundTransactionRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
import com.billing.payments_core_api.util.FormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundTransactionStripeFailureHandler implements StripeFailureHandler<RefundTransaction> {

    private final RefundTransactionRepository refundTransactionRepository;
    private final PaymentNotificationService notificationService;
    private final FormatterUtil formatterUtil;

    @Override
    public void handle(RefundTransaction refundTransaction, ExternalServiceException e) {
        log.error("m=handle, refundTransactionId={}, error={}", refundTransaction.getId(), e.getMessage());
        refundTransaction.setStatus(RefundStatus.FAILED);
        refundTransaction.setFailureReason(formatterUtil.truncateMessage(e.getMessage()));
        refundTransactionRepository.save(refundTransaction);
        notificationService.notifyRefundProcessed(refundTransaction);
    }

}
