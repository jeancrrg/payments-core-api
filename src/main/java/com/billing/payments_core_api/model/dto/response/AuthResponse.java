package com.billing.payments_core_api.model.dto.response;

public record AuthResponse(String token, String type, long expiresIn) {

    public static AuthResponse bearer(String token, long expiresInMs) {
        return new AuthResponse(token, "Bearer", expiresInMs);
    }
}
