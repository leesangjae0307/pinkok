package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.PinlogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pinlogs")
public class PinlogController {

    private final PinlogService pinlogService;

    public PinlogController(PinlogService pinlogService) {
        this.pinlogService = pinlogService;
    }

    // TODO: API 엔드포인트 작성
}
