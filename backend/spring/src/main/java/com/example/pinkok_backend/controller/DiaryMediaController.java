package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.DiaryMediaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diary-media")
public class DiaryMediaController {

    private final DiaryMediaService diaryMediaService;

    public DiaryMediaController(DiaryMediaService diaryMediaService) {
        this.diaryMediaService = diaryMediaService;
    }

    // TODO: API 엔드포인트 작성
}
