package com.billing.payments_core_api.controller;

import com.billing.payments_core_api.controller.docs.CustomerApi;
import com.billing.payments_core_api.model.dto.request.CustomerRequest;
import com.billing.payments_core_api.model.dto.response.CustomerResponse;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponse>> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(customerService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        CustomerResponse response = customerService.create(request);
        URI location = uriBuilder.path("/v1/customers/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
