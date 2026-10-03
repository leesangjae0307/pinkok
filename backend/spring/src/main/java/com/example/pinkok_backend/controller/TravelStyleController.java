package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.TravelStyleResponse;
import com.example.pinkok_backend.service.TravelStyleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/travel-styles")
public class TravelStyleController {

    private final TravelStyleService travelStyleService;

    public TravelStyleController(TravelStyleService travelStyleService) {
        this.travelStyleService = travelStyleService;
    }

    /** 여행 스타일 목록 (FOOD / SIGHT / NATURE / SHOPPING / CAFE). 추천 요청의 styleCodes 에 이 code 를 쓴다. */
    @GetMapping
    public List<TravelStyleResponse> list() {
        return travelStyleService.list();
    }
}
