package com.billing.payments_core_api.controller;

import com.billing.payments_core_api.controller.docs.RefundsApi;
import com.billing.payments_core_api.model.dto.request.RefundRequest;
import com.billing.payments_core_api.model.dto.response.RefundResponse;
import com.billing.payments_core_api.service.RefundService;
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
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController implements RefundsApi {

    private final RefundService refundService;

    @PostMapping
    @Override
    public ResponseEntity<RefundResponse> create(@Valid @RequestBody RefundRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        RefundResponse response = refundService.requestRefund(request);
        URI location = uriBuilder.path("/api/v1/refunds/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<RefundResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(refundService.findById(id));
    }

    @GetMapping("/payment/{paymentId}")
    @Override
    public ResponseEntity<List<RefundResponse>> findByPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(refundService.findByPaymentId(paymentId));
    }
}
