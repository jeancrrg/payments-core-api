package com.billing.payments_core_api.controller;

import com.billing.payments_core_api.controller.docs.AboutApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/v1/about")
public class AboutController implements AboutApi {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.profiles.active:default}")
    private String profile;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", appName,
                "profile", profile,
                "timestamp", OffsetDateTime.now()
        ));
    }

}
