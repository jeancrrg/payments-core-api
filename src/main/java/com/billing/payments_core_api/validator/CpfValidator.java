package com.billing.payments_core_api.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    private static final String UNMASKED_PATTERN = "[0-9]{11}";
    private static final String MASKED_PATTERN = "[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        if (!hasValidFormat(value)) {
            return false;
        }
        String digits = stripMask(value);
        return isNotAllSameDigit(digits) && hasValidCheckDigits(digits);
    }

    private boolean hasValidFormat(String cpf) {
        return cpf.matches(UNMASKED_PATTERN) || cpf.matches(MASKED_PATTERN);
    }

    private String stripMask(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }

    private boolean isNotAllSameDigit(String digits) {
        return digits.chars().distinct().count() > 1;
    }

    private boolean hasValidCheckDigits(String digits) {
        return calculateCheckDigit(digits, 9) == digitAt(digits, 9)
                && calculateCheckDigit(digits, 10) == digitAt(digits, 10);
    }

    private int calculateCheckDigit(String digits, int position) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += digitAt(digits, i) * (position + 1 - i);
        }
        int remainder = 11 - (sum % 11);
        return remainder >= 10 ? 0 : remainder;
    }

    private int digitAt(String digits, int index) {
        return digits.charAt(index) - '0';
    }

}
