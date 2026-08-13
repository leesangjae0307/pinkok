package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.PlaceCandidateRepository;
import org.springframework.stereotype.Service;

@Service
public class PlaceCandidateService {

    private final PlaceCandidateRepository placeCandidateRepository;

    public PlaceCandidateService(PlaceCandidateRepository placeCandidateRepository) {
        this.placeCandidateRepository = placeCandidateRepository;
    }

    // TODO: 비즈니스 로직 작성
}
