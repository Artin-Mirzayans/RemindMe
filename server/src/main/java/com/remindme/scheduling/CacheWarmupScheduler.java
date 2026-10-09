package com.remindme.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remindme.digest.DigestService;
import com.remindme.watchlist.WatchlistService;

@Component
public class CacheWarmupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupScheduler.class);

    private final DigestService digestService;
    private final WatchlistService watchlistService;

    public CacheWarmupScheduler(DigestService digestService, WatchlistService watchlistService) {
        this.digestService = digestService;
        this.watchlistService = watchlistService;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void warmCaches() {
        log.info("Warming digest and watchlist caches");
        digestService.today();
        // true so it refreshes once the 3 day cooldown is up instead of serving the old feed
        // until someone presses Refresh - inside the cooldown it's a no-op
        watchlistService.current(true);
    }
}
