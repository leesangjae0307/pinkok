package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.AiRequestImageRepository;
import org.springframework.stereotype.Service;

@Service
public class AiRequestImageService {

    private final AiRequestImageRepository aiRequestImageRepository;

    public AiRequestImageService(AiRequestImageRepository aiRequestImageRepository) {
        this.aiRequestImageRepository = aiRequestImageRepository;
    }

    // TODO: 비즈니스 로직 작성
}
