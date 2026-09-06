package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.TripInvitationRepository;
import org.springframework.stereotype.Service;

@Service
public class TripInvitationService {

    private final TripInvitationRepository tripInvitationRepository;

    public TripInvitationService(TripInvitationRepository tripInvitationRepository) {
        this.tripInvitationRepository = tripInvitationRepository;
    }

    // TODO: 비즈니스 로직 작성
}
