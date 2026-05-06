package com.billing.payments_core_api.mapper;

import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import com.billing.payments_core_api.model.dto.response.RefundResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment entity);

    List<PaymentResponse> toResponseList(List<Payment> entities);

    @Mapping(target = "paymentId", source = "payment.id")
    RefundResponse toRefundResponse(Refund entity);

    List<RefundResponse> toRefundResponseList(List<Refund> entities);
}
