package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.TripRepository;
import org.springframework.stereotype.Service;

@Service
public class TripService {

    private final TripRepository tripRepository;

    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    // TODO: 비즈니스 로직 작성
}
