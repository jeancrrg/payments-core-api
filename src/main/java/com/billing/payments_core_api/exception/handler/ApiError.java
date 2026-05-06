package com.billing.payments_core_api.exception.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Standard error envelope returned by the API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @Schema(description = "Error timestamp") OffsetDateTime timestamp,
        @Schema(description = "HTTP status code") int status,
        @Schema(description = "HTTP status description") String error,
        @Schema(description = "Application-specific error code") String code,
        @Schema(description = "Human-readable error message") String message,
        @Schema(description = "Originating request path") String path,
        @Schema(description = "Per-field validation messages") List<FieldError> fieldErrors
) {

    public static ApiError of(int status, String error, String code, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, code, message, path, null);
    }

    public static ApiError of(int status, String error, String code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(OffsetDateTime.now(), status, error, code, message, path, fieldErrors);
    }

    @Schema(description = "Validation error for a single field")
    public record FieldError(String field, String message) {
    }
}
