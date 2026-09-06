package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.PinlogRepository;
import org.springframework.stereotype.Service;

@Service
public class PinlogService {

    private final PinlogRepository pinlogRepository;

    public PinlogService(PinlogRepository pinlogRepository) {
        this.pinlogRepository = pinlogRepository;
    }

    // TODO: 비즈니스 로직 작성
}
