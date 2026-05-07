package com.billing.payments_core_api.mapper;

import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import com.billing.payments_core_api.model.dto.response.PaymentStatusResponse;
import com.billing.payments_core_api.model.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment entity);

    PaymentStatusResponse toStatusResponse(Payment entity);

}
