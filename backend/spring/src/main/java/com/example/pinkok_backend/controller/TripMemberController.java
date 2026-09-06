package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.TripMemberService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trip-members")
public class TripMemberController {

    private final TripMemberService tripMemberService;

    public TripMemberController(TripMemberService tripMemberService) {
        this.tripMemberService = tripMemberService;
    }

    // TODO: API 엔드포인트 작성
}
