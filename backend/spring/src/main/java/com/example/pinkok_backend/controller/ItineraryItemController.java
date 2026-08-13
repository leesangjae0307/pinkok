package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.ItineraryItemService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/itinerary-items")
public class ItineraryItemController {

    private final ItineraryItemService itineraryItemService;

    public ItineraryItemController(ItineraryItemService itineraryItemService) {
        this.itineraryItemService = itineraryItemService;
    }

    // TODO: API 엔드포인트 작성
}
