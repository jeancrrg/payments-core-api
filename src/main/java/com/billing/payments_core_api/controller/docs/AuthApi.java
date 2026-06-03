package com.billing.payments_core_api.controller.docs;

import com.billing.payments_core_api.model.dto.request.AuthRequest;
import com.billing.payments_core_api.model.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "Operations to authenticate and obtain access tokens")
public interface AuthApi {

    @Operation(summary = "Login", description = "Authenticates with username and password. Returns a Bearer JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or blank fields"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request);

}
