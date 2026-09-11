package com.remindme.local;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoLocationTest {

    @Test
    @DisplayName("label combines city and region when both are present")
    void labelWithCityAndRegion() {
        GeoLocation loc = new GeoLocation("Calabasas", "California", "United States", 34.14, -118.66,
                "America/Los_Angeles");
        assertThat(loc.label()).isEqualTo("Calabasas, California");
    }

    @Test
    @DisplayName("label falls back to region, then country, when city is missing")
    void labelFallsBack() {
        assertThat(new GeoLocation(null, "Texas", "United States", 0, 0, null).label()).isEqualTo("Texas");
        assertThat(new GeoLocation("", "", "Ireland", 0, 0, null).label()).isEqualTo("Ireland");
    }
}
