package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.AiRequestService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-requests")
public class AiRequestController {

    private final AiRequestService aiRequestService;

    public AiRequestController(AiRequestService aiRequestService) {
        this.aiRequestService = aiRequestService;
    }

    // TODO: API 엔드포인트 작성
}
