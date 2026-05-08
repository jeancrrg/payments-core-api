package com.billing.payments_core_api.controller.docs;

import com.billing.payments_core_api.model.dto.request.RefundTransactionRequest;
import com.billing.payments_core_api.model.dto.response.RefundTransactionResponse;
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

@Tag(name = "Refund Transactions", description = "Operations to request and consult refund transactions via Stripe")
public interface RefundTransactionApi {

    @Operation(summary = "Find refund transaction by id", description = "Returns a single refund transaction. Cached in Redis.")
    ResponseEntity<RefundTransactionResponse> findById(@Parameter(description = "Refund transaction id") @PathVariable UUID id);

    @Operation(summary = "List refund transactions for a payment")
    ResponseEntity<List<RefundTransactionResponse>> findByPayment(@PathVariable UUID paymentId);

    @Operation(summary = "Request a refund transaction", description = "Issues a refund through Stripe (with retry). " +
            "If amount is omitted, the remaining refundable amount is refunded.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Refund transaction created"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "422", description = "Refund not allowed (invalid status or amount)"),
            @ApiResponse(responseCode = "502", description = "Stripe gateway error"),
            @ApiResponse(responseCode = "503", description = "Stripe temporarily unavailable (circuit breaker open)")
    })
    ResponseEntity<RefundTransactionResponse> create(@Valid @RequestBody RefundTransactionRequest request, UriComponentsBuilder uriBuilder);

}
