package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.AvatarRepository;
import org.springframework.stereotype.Service;

@Service
public class AvatarService {

    private final AvatarRepository avatarRepository;

    public AvatarService(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    // TODO: 비즈니스 로직 작성
}
