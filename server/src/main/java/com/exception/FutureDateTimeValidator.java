package com.exception;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FutureDateTimeValidator implements ConstraintValidator<FutureDateTime, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public void initialize(FutureDateTime constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // @NotNull should handle null cases
        }

        try {
            ZonedDateTime parsedDateTime = ZonedDateTime.parse(value, FORMATTER.withZone(ZoneId.of("UTC")));
            Instant parsedInstant = parsedDateTime.toInstant();

            Instant now = Instant.now();

            return parsedInstant.isAfter(now);
        } catch (DateTimeParseException e) {
            return false; // Invalid format
        }
    }
}
