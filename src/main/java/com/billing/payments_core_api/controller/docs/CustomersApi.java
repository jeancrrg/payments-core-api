package com.billing.payments_core_api.controller.docs;

import com.billing.payments_core_api.model.dto.request.CustomerRequest;
import com.billing.payments_core_api.model.dto.response.CustomerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Tag(name = "Customers", description = "Operations to create and manage customers")
public interface CustomersApi {

    @Operation(summary = "Create a new customer", description = "Registers a customer with name and CPF (unique, validated). Returns 201 with the created customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "CPF already registered")
    })
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request,
                                            UriComponentsBuilder uriBuilder);

    @Operation(summary = "Find customer by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    ResponseEntity<CustomerResponse> findById(
            @Parameter(description = "Customer id") @PathVariable UUID id);

    @Operation(summary = "Find customer by CPF", description = "Accepts CPF with or without formatting (dots/dash stripped).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    ResponseEntity<CustomerResponse> findByCpf(
            @Parameter(description = "CPF (11 digits, formatted or raw)") @PathVariable String cpf);

    @Operation(summary = "Update customer", description = "Updates name and CPF. CPF must be valid and not already used by another customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "CPF already in use by another customer")
    })
    ResponseEntity<CustomerResponse> update(
            @Parameter(description = "Customer id") @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request);

    @Operation(summary = "Delete customer", description = "Deletes customer. Returns 409 if customer has payments.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Customer deleted"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Customer has existing payments")
    })
    ResponseEntity<Void> delete(
            @Parameter(description = "Customer id") @PathVariable UUID id);
}
