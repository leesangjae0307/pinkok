package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.TripTravelStyleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trip-travel-styles")
public class TripTravelStyleController {

    private final TripTravelStyleService tripTravelStyleService;

    public TripTravelStyleController(TripTravelStyleService tripTravelStyleService) {
        this.tripTravelStyleService = tripTravelStyleService;
    }

    // TODO: API 엔드포인트 작성
}
