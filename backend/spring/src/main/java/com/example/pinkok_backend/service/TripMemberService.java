package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.TripMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class TripMemberService {

    private final TripMemberRepository tripMemberRepository;

    public TripMemberService(TripMemberRepository tripMemberRepository) {
        this.tripMemberRepository = tripMemberRepository;
    }

    // TODO: 비즈니스 로직 작성
}
