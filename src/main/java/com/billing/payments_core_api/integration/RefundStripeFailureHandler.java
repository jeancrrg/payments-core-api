package com.billing.payments_core_api.integration;

import com.billing.payments_core_api.exception.ExternalServiceException;
import com.billing.payments_core_api.model.entity.Refund;
import com.billing.payments_core_api.model.enums.RefundStatus;
import com.billing.payments_core_api.repository.RefundRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
import com.billing.payments_core_api.util.FormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundStripeFailureHandler implements StripeFailureHandler<Refund> {

    private final RefundRepository refundRepository;
    private final PaymentNotificationService notificationService;
    private final FormatterUtil formatterUtil;

    @Override
    public void handle(Refund refund, ExternalServiceException e) {
        log.error("m=handle, refundId={}, error={}", refund.getId(), e.getMessage());
        refund.setStatus(RefundStatus.FAILED);
        refund.setFailureReason(formatterUtil.truncateMessage(e.getMessage()));
        refundRepository.save(refund);
        notificationService.notifyRefundProcessed(refund);
    }

}
