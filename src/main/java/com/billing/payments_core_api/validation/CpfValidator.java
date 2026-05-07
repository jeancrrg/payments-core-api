package com.billing.payments_core_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    private static final String DIGITS_ONLY_PATTERN = "[0-9]{11}";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return hasValidFormat(value)
                && isNotAllSameDigit(value)
                && hasValidCheckDigits(value);
    }

    private boolean hasValidFormat(String cpf) {
        return cpf.matches(DIGITS_ONLY_PATTERN);
    }

    private boolean isNotAllSameDigit(String cpf) {
        return cpf.chars().distinct().count() > 1;
    }

    private boolean hasValidCheckDigits(String cpf) {
        return calculateCheckDigit(cpf, 9) == digitAt(cpf, 9)
                && calculateCheckDigit(cpf, 10) == digitAt(cpf, 10);
    }

    private int calculateCheckDigit(String cpf, int position) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += digitAt(cpf, i) * (position + 1 - i);
        }
        int remainder = 11 - (sum % 11);
        return remainder >= 10 ? 0 : remainder;
    }

    private int digitAt(String cpf, int index) {
        return cpf.charAt(index) - '0';
    }

}
