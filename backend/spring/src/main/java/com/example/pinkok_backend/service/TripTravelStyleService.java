package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.TripTravelStyleRepository;
import org.springframework.stereotype.Service;

@Service
public class TripTravelStyleService {

    private final TripTravelStyleRepository tripTravelStyleRepository;

    public TripTravelStyleService(TripTravelStyleRepository tripTravelStyleRepository) {
        this.tripTravelStyleRepository = tripTravelStyleRepository;
    }

    // TODO: 비즈니스 로직 작성
}
