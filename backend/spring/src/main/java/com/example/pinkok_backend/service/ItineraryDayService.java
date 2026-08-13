package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.ItineraryDayRepository;
import org.springframework.stereotype.Service;

@Service
public class ItineraryDayService {

    private final ItineraryDayRepository itineraryDayRepository;

    public ItineraryDayService(ItineraryDayRepository itineraryDayRepository) {
        this.itineraryDayRepository = itineraryDayRepository;
    }

    // TODO: 비즈니스 로직 작성
}
