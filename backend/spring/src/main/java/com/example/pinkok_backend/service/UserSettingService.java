package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.UserSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;

    public UserSettingService(UserSettingRepository userSettingRepository) {
        this.userSettingRepository = userSettingRepository;
    }

    // TODO: 비즈니스 로직 작성
}
