package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.TravelStyleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/travel-styles")
public class TravelStyleController {

    private final TravelStyleService travelStyleService;

    public TravelStyleController(TravelStyleService travelStyleService) {
        this.travelStyleService = travelStyleService;
    }

    // TODO: API 엔드포인트 작성
}
