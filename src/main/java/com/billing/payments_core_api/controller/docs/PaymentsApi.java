package com.billing.payments_core_api.controller.docs;

import com.billing.payments_core_api.model.dto.request.PaymentRequest;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Payments", description = "Operations to create and consult payments")
public interface PaymentsApi {

    @Operation(summary = "Create a new payment", description = "Creates a payment via Stripe (with Resilience4j retry). Returns 201 with the persisted payment.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "502", description = "Stripe gateway error"),
            @ApiResponse(responseCode = "503", description = "Stripe temporarily unavailable (circuit breaker open)")
    })
    ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request,
                                           UriComponentsBuilder uriBuilder);

    @Operation(summary = "Find payment by id", description = "Returns the payment. Result is served from Redis cache when available.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    ResponseEntity<PaymentResponse> findById(
            @Parameter(description = "Payment id") @PathVariable UUID id);

    @Operation(summary = "Get current payment status", description = "Returns only the current PaymentStatus.")
    ResponseEntity<Map<String, Object>> status(@PathVariable UUID id);

    @Operation(summary = "Re-sync payment with Stripe", description = "Calls Stripe to refresh the payment status (with retry). Invalidates the cache.")
    ResponseEntity<PaymentResponse> sync(@PathVariable UUID id);

    @Operation(summary = "List payments by customer", description = "Paginated list of customer payments. Cached in Redis.")
    @ApiResponse(responseCode = "200", description = "Page of payments")
    ResponseEntity<PageResponse<PaymentResponse>> findByCustomer(
            @Parameter(description = "Customer id") @PathVariable UUID customerId,
            @Parameter(hidden = true) Pageable pageable);
}
