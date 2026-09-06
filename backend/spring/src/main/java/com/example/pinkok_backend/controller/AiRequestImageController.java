package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.AiRequestImageService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-request-images")
public class AiRequestImageController {

    private final AiRequestImageService aiRequestImageService;

    public AiRequestImageController(AiRequestImageService aiRequestImageService) {
        this.aiRequestImageService = aiRequestImageService;
    }

    // TODO: API 엔드포인트 작성
}
