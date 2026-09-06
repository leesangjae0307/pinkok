package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.TravelStyleRepository;
import org.springframework.stereotype.Service;

@Service
public class TravelStyleService {

    private final TravelStyleRepository travelStyleRepository;

    public TravelStyleService(TravelStyleRepository travelStyleRepository) {
        this.travelStyleRepository = travelStyleRepository;
    }

    // TODO: 비즈니스 로직 작성
}
