package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.DiaryRepository;
import org.springframework.stereotype.Service;

@Service
public class DiaryService {

    private final DiaryRepository diaryRepository;

    public DiaryService(DiaryRepository diaryRepository) {
        this.diaryRepository = diaryRepository;
    }

    // TODO: 비즈니스 로직 작성
}
