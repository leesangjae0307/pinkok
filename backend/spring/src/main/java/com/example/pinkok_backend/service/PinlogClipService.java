package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.PinlogClipRepository;
import org.springframework.stereotype.Service;

@Service
public class PinlogClipService {

    private final PinlogClipRepository pinlogClipRepository;

    public PinlogClipService(PinlogClipRepository pinlogClipRepository) {
        this.pinlogClipRepository = pinlogClipRepository;
    }

    // TODO: 비즈니스 로직 작성
}
