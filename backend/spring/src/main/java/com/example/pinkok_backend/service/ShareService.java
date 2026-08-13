package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.ShareRepository;
import org.springframework.stereotype.Service;

@Service
public class ShareService {

    private final ShareRepository shareRepository;

    public ShareService(ShareRepository shareRepository) {
        this.shareRepository = shareRepository;
    }

    // TODO: 비즈니스 로직 작성
}
