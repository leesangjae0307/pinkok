package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.PlaceCandidateService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/place-candidates")
public class PlaceCandidateController {

    private final PlaceCandidateService placeCandidateService;

    public PlaceCandidateController(PlaceCandidateService placeCandidateService) {
        this.placeCandidateService = placeCandidateService;
    }

    // TODO: API 엔드포인트 작성
}
