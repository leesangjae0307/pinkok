package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.PinlogClipService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pinlog-clips")
public class PinlogClipController {

    private final PinlogClipService pinlogClipService;

    public PinlogClipController(PinlogClipService pinlogClipService) {
        this.pinlogClipService = pinlogClipService;
    }

    // TODO: API 엔드포인트 작성
}
