package com.remindme.services;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.remindme.repositories.WatchlistInterestRepository;

@Service
public class WatchlistInterestService {

    private final WatchlistInterestRepository watchlistInterestRepository;

    public WatchlistInterestService(WatchlistInterestRepository watchlistInterestRepository) {
        this.watchlistInterestRepository = watchlistInterestRepository;
    }

    public boolean recordInterest(String userId, String category) {
        return watchlistInterestRepository.recordInterest(userId, category);
    }

    public Map<String, Integer> getInterestCounts(String userId) {
        return watchlistInterestRepository.getInterestCounts(userId);
    }
}
