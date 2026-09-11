package com.remindme.watchlist;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remindme.services.WatchlistInterestService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/watchlist")
@Validated
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final WatchlistInterestService watchlistInterestService;
    private final WatchlistRanker watchlistRanker;

    public WatchlistController(WatchlistService watchlistService, WatchlistInterestService watchlistInterestService,
            WatchlistRanker watchlistRanker) {
        this.watchlistService = watchlistService;
        this.watchlistInterestService = watchlistInterestService;
        this.watchlistRanker = watchlistRanker;
    }

    @GetMapping
    public ResponseEntity<WatchlistWindow> get(HttpServletRequest request,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        String userId = (String) request.getAttribute("userId");

        WatchlistWindow window = watchlistService.current(refresh);
        Map<String, Integer> interestCounts = watchlistInterestService.getInterestCounts(userId);
        List<WatchlistEvent> ranked = watchlistRanker.rank(window.events(), interestCounts);

        return ResponseEntity.ok(
                new WatchlistWindow(window.windowStart(), window.windowEnd(), ranked, window.nextRefreshAt()));
    }

    @PostMapping("/interest")
    public ResponseEntity<String> logInterest(HttpServletRequest request,
            @Valid @RequestBody WatchlistInterestRequest body) {
        String userId = (String) request.getAttribute("userId");

        watchlistInterestService.recordInterest(userId, body.category());

        return ResponseEntity.ok("Interest recorded");
    }
}
