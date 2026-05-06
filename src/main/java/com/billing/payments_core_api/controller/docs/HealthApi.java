package com.billing.payments_core_api.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "Health", description = "Liveness check")
public interface HealthApi {

    @Operation(summary = "Liveness check")
    ResponseEntity<Map<String, Object>> health();
}
