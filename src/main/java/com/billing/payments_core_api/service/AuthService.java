package com.billing.payments_core_api.service;

import com.billing.payments_core_api.model.dto.request.AuthRequest;
import com.billing.payments_core_api.model.dto.response.AuthResponse;
import com.billing.payments_core_api.config.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserDetails user = userService.findByUsername(request.username());
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, expirationMs);
    }

}
