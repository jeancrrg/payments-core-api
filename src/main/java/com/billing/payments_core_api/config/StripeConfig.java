package com.billing.payments_core_api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.api.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${stripe.api.read-timeout-ms:30000}")
    private int readTimeoutMs;

    @PostConstruct
    void init() {
        if (stripeApiKey == null || stripeApiKey.isBlank() || "sk_test_replace_me".equals(stripeApiKey)) {
            log.warn("Stripe API key is using the placeholder value. Set STRIPE_API_KEY env var with a real sk_test_ key.");
        }
        Stripe.apiKey = stripeApiKey;
        Stripe.setConnectTimeout(connectTimeoutMs);
        Stripe.setReadTimeout(readTimeoutMs);
        log.info("Stripe SDK initialized (connectTimeout={}ms, readTimeout={}ms)", connectTimeoutMs, readTimeoutMs);
    }
}
