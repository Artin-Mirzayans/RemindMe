package com.remindme.evals;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// public and read-only - everything here is aggregate operational data (request counts,
// success rates, cost totals), nothing user-specific, so it's fine to expose without auth
@RestController
@RequestMapping("/evals")
public class EvalsController {

    private static final int MAX_WINDOW_DAYS = 90;

    private final EvalsService evalsService;

    public EvalsController(EvalsService evalsService) {
        this.evalsService = evalsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<List<EvalsSummary>> summary(
            @RequestParam(name = "days", defaultValue = "30") int days) {
        int window = Math.max(1, Math.min(days, MAX_WINDOW_DAYS));
        return ResponseEntity.ok(evalsService.summarizeAll(window));
    }
}
