package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.VlogRepository;
import org.springframework.stereotype.Service;

@Service
public class VlogService {

    private final VlogRepository vlogRepository;

    public VlogService(VlogRepository vlogRepository) {
        this.vlogRepository = vlogRepository;
    }

    // TODO: 비즈니스 로직 작성
}
