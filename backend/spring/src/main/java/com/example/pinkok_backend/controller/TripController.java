package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.TripService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // TODO: API 엔드포인트 작성
}
