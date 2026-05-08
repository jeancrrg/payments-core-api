package com.billing.payments_core_api.model.mapper;

import com.billing.payments_core_api.model.dto.response.RefundTransactionResponse;
import com.billing.payments_core_api.model.entity.RefundTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RefundTransactionMapper {

    RefundTransactionResponse toRefundTransactionResponse(RefundTransaction entity);

    List<RefundTransactionResponse> toRefundTransactionResponseList(List<RefundTransaction> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "stripeRefundId", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RefundTransaction toEntity(UUID paymentId, BigDecimal amount, String reason);

}
