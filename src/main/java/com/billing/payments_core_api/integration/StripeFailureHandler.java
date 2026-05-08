package com.billing.payments_core_api.integration;

import com.billing.payments_core_api.exception.ExternalServiceException;

public interface StripeFailureHandler<T> {

    void handle(T entity, ExternalServiceException ex);

}
