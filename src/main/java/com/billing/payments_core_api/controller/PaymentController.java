package com.billing.payments_core_api.controller;

import com.billing.payments_core_api.controller.docs.PaymentsApi;
import com.billing.payments_core_api.model.dto.request.PaymentRequest;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentsApi {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        PaymentResponse response = paymentService.createPayment(request);
        URI location = uriBuilder.path("/v1/payments/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable UUID id) {
        PaymentStatus status = paymentService.getStatus(id);
        return ResponseEntity.ok(Map.of("id", id, "status", status));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<PaymentResponse> sync(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.syncWithStripe(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<PageResponse<PaymentResponse>> findByCustomer(@PathVariable UUID customerId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.findByCustomer(customerId, pageable));
    }
}
