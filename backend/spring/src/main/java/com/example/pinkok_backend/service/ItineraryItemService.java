package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.ItineraryItemRepository;
import org.springframework.stereotype.Service;

@Service
public class ItineraryItemService {

    private final ItineraryItemRepository itineraryItemRepository;

    public ItineraryItemService(ItineraryItemRepository itineraryItemRepository) {
        this.itineraryItemRepository = itineraryItemRepository;
    }

    // TODO: 비즈니스 로직 작성
}
