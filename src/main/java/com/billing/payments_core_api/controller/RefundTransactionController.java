package com.billing.payments_core_api.controller;

import com.billing.payments_core_api.controller.docs.RefundTransactionApi;
import com.billing.payments_core_api.model.dto.request.RefundTransactionRequest;
import com.billing.payments_core_api.model.dto.response.RefundTransactionResponse;
import com.billing.payments_core_api.service.RefundTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/refunds")
@RequiredArgsConstructor
public class RefundTransactionController implements RefundTransactionApi {

    private final RefundTransactionService refundTransactionService;

    @GetMapping("/{id}")
    public ResponseEntity<RefundTransactionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(refundTransactionService.findById(id));
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<RefundTransactionResponse>> findByPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(refundTransactionService.findByPaymentId(paymentId));
    }

    @PostMapping
    public ResponseEntity<RefundTransactionResponse> create(@Valid @RequestBody RefundTransactionRequest request, UriComponentsBuilder uriBuilder) {
        RefundTransactionResponse response = refundTransactionService.requestRefund(request);
        URI location = uriBuilder.path("/v1/refunds/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

}
