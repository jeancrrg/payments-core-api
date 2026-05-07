package com.billing.payments_core_api.controller.docs;

import com.billing.payments_core_api.model.dto.request.RefundRequest;
import com.billing.payments_core_api.model.dto.response.RefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Tag(name = "Refunds", description = "Operations to request and consult refunds via Stripe")
public interface RefundApi {

    @Operation(summary = "Request a refund", description = "Issues a refund through Stripe (with retry). If amount is omitted, the remaining refundable amount is refunded.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Refund created"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "422", description = "Refund not allowed (invalid status or amount)"),
            @ApiResponse(responseCode = "502", description = "Stripe gateway error"),
            @ApiResponse(responseCode = "503", description = "Stripe temporarily unavailable (circuit breaker open)")
    })
    ResponseEntity<RefundResponse> create(@Valid @RequestBody RefundRequest request,
                                          UriComponentsBuilder uriBuilder);

    @Operation(summary = "Find refund by id", description = "Returns a single refund. Cached in Redis.")
    ResponseEntity<RefundResponse> findById(
            @Parameter(description = "Refund id") @PathVariable UUID id);

    @Operation(summary = "List refunds for a payment")
    ResponseEntity<List<RefundResponse>> findByPayment(@PathVariable UUID paymentId);
}
