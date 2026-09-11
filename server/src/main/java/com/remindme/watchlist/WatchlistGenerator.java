package com.remindme.watchlist;

import java.time.LocalDate;

public interface WatchlistGenerator {
    WatchlistWindow generate(LocalDate windowStart, LocalDate windowEnd);
}
