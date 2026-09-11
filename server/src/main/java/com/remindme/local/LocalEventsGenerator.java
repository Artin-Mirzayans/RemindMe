package com.remindme.local;

import java.time.LocalDate;

public interface LocalEventsGenerator {
    LocalEventsWindow generate(GeoLocation location, LocalDate windowStart, LocalDate windowEnd);
}
