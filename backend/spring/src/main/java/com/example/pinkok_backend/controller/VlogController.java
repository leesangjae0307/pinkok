package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.VlogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vlogs")
public class VlogController {

    private final VlogService vlogService;

    public VlogController(VlogService vlogService) {
        this.vlogService = vlogService;
    }

    // TODO: API 엔드포인트 작성
}
