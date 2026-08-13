package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.RecommendationRepository;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;

    public RecommendationService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    // TODO: 비즈니스 로직 작성
}
