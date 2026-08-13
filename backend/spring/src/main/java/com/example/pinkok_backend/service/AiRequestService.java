package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.AiRequestRepository;
import org.springframework.stereotype.Service;

@Service
public class AiRequestService {

    private final AiRequestRepository aiRequestRepository;

    public AiRequestService(AiRequestRepository aiRequestRepository) {
        this.aiRequestRepository = aiRequestRepository;
    }

    // TODO: 비즈니스 로직 작성
}
