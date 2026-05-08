package com.billing.payments_core_api.model.mapper;

import com.billing.payments_core_api.model.dto.response.RefundResponse;
import com.billing.payments_core_api.model.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    RefundResponse toRefundResponse(Refund entity);

    List<RefundResponse> toRefundResponseList(List<Refund> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "stripeRefundId", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Refund toEntity(UUID paymentId, BigDecimal amount, String reason);

}
