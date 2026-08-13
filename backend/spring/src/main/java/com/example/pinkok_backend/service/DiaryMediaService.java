package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.DiaryMediaRepository;
import org.springframework.stereotype.Service;

@Service
public class DiaryMediaService {

    private final DiaryMediaRepository diaryMediaRepository;

    public DiaryMediaService(DiaryMediaRepository diaryMediaRepository) {
        this.diaryMediaRepository = diaryMediaRepository;
    }

    // TODO: 비즈니스 로직 작성
}
