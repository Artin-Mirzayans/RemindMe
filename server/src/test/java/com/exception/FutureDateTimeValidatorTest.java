package com.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FutureDateTimeValidatorTest {

    private final FutureDateTimeValidator validator = new FutureDateTimeValidator();

    private String offsetFromNow(long minutes) {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .plusMinutes(minutes)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    @Test
    @DisplayName("accepts a time in the future")
    void acceptsFutureTime() {
        assertThat(validator.isValid(offsetFromNow(60), null)).isTrue();
    }

    @Test
    @DisplayName("rejects a time in the past")
    void rejectsPastTime() {
        assertThat(validator.isValid(offsetFromNow(-60), null)).isFalse();
    }

    @Test
    @DisplayName("rejects a value that is not a timestamp")
    void rejectsGarbage() {
        assertThat(validator.isValid("not-a-date", null)).isFalse();
    }
}
