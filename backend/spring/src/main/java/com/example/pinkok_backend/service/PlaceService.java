package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.PlaceRepository;
import org.springframework.stereotype.Service;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    // TODO: 비즈니스 로직 작성
}
