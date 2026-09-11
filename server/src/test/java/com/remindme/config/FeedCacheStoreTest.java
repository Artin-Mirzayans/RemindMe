package com.remindme.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

class FeedCacheStoreTest {

    record Sample(String label, List<Integer> values) {
    }

    @Test
    @DisplayName("saves a value and reads it back")
    void roundTrips() {
        FeedCacheStore store = FeedCacheStore.inMemory();

        store.save("thing", new Sample("hi", List.of(1, 2, 3)));

        assertThat(store.load("thing", Sample.class)).contains(new Sample("hi", List.of(1, 2, 3)));
    }

    @Test
    @DisplayName("round-trips a generic map via TypeReference")
    void roundTripsAMap() {
        FeedCacheStore store = FeedCacheStore.inMemory();

        store.save("map", Map.of("a", new Sample("x", List.of(9))));

        Map<String, Sample> back = store.load("map", new TypeReference<Map<String, Sample>>() {
        }).orElseThrow();
        assertThat(back).containsEntry("a", new Sample("x", List.of(9)));
    }

    @Test
    @DisplayName("returns empty for a name that was never saved")
    void missingIsEmpty() {
        assertThat(FeedCacheStore.inMemory().load("nope", Sample.class)).isEmpty();
    }

    @Test
    @DisplayName("the disabled store never persists or reads anything")
    void disabledIsInert() {
        FeedCacheStore disabled = FeedCacheStore.disabled();

        disabled.save("thing", new Sample("hi", List.of(1)));

        assertThat(disabled.load("thing", Sample.class)).isEmpty();
    }
}
