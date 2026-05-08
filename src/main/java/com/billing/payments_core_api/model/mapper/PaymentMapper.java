package com.billing.payments_core_api.model.mapper;

import com.billing.payments_core_api.model.dto.request.PaymentRequest;
import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import com.billing.payments_core_api.model.dto.response.PaymentStatusResponse;
import com.billing.payments_core_api.model.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment entity);

    PaymentStatusResponse toStatusResponse(Payment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currency", expression = "java(request.currency().toUpperCase())")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "stripePaymentIntentId", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toEntity(PaymentRequest request);

}
