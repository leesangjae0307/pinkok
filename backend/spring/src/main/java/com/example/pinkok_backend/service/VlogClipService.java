package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.VlogClipRepository;
import org.springframework.stereotype.Service;

@Service
public class VlogClipService {

    private final VlogClipRepository vlogClipRepository;

    public VlogClipService(VlogClipRepository vlogClipRepository) {
        this.vlogClipRepository = vlogClipRepository;
    }

    // TODO: 비즈니스 로직 작성
}
