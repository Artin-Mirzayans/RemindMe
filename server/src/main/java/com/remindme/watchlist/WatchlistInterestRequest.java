package com.remindme.watchlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchlistInterestRequest(
        @NotBlank @Size(max = 50) String category) {
}
