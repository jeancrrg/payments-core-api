package com.billing.payments_core_api.exception.handler;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        List<FieldError> errors
) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, LocalDateTime.now(), null);
    }

    public static ApiError of(int status, String error, String message, List<FieldError> errors) {
        return new ApiError(status, error, message, LocalDateTime.now(), errors);
    }

}
