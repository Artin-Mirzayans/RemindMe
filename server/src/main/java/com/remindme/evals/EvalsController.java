package com.remindme.evals;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// public on purpose, so it only ever returns rates and typical timings - the raw counts and
// spend stay in DynamoDB and the logs
@RestController
@RequestMapping("/evals")
public class EvalsController {

    private static final int MAX_WINDOW_DAYS = 90;

    private final EvalsService evalsService;

    public EvalsController(EvalsService evalsService) {
        this.evalsService = evalsService;
    }

    @GetMapping("/health")
    public ResponseEntity<List<FeedHealth>> health(
            @RequestParam(name = "days", defaultValue = "30") int days) {
        int window = Math.max(1, Math.min(days, MAX_WINDOW_DAYS));
        return ResponseEntity.ok(evalsService.health(window));
    }
}
