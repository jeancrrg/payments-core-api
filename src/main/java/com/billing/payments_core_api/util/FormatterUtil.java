package com.billing.payments_core_api.util;

import org.springframework.stereotype.Component;

@Component
public class FormatterUtil {

    private static final int MAX_FAILURE_REASON_LENGTH = 512;

    public String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_FAILURE_REASON_LENGTH ? message : message.substring(0, MAX_FAILURE_REASON_LENGTH);
    }

}
