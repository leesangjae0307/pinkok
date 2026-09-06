package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.TripInvitationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trip-invitations")
public class TripInvitationController {

    private final TripInvitationService tripInvitationService;

    public TripInvitationController(TripInvitationService tripInvitationService) {
        this.tripInvitationService = tripInvitationService;
    }

    // TODO: API 엔드포인트 작성
}
