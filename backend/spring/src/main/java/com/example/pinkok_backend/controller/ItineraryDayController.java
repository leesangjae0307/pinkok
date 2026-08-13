package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.ItineraryDayService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/itinerary-days")
public class ItineraryDayController {

    private final ItineraryDayService itineraryDayService;

    public ItineraryDayController(ItineraryDayService itineraryDayService) {
        this.itineraryDayService = itineraryDayService;
    }

    // TODO: API 엔드포인트 작성
}
