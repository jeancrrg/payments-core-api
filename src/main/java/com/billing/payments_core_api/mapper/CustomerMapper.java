package com.billing.payments_core_api.mapper;

import com.billing.payments_core_api.model.dto.response.CustomerResponse;
import com.billing.payments_core_api.model.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer entity);
}
