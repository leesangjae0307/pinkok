package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.UserTravelStyleRepository;
import org.springframework.stereotype.Service;

@Service
public class UserTravelStyleService {

    private final UserTravelStyleRepository userTravelStyleRepository;

    public UserTravelStyleService(UserTravelStyleRepository userTravelStyleRepository) {
        this.userTravelStyleRepository = userTravelStyleRepository;
    }

    // TODO: 비즈니스 로직 작성
}
