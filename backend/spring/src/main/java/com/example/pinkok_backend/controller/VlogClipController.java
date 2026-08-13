package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.VlogClipService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vlog-clips")
public class VlogClipController {

    private final VlogClipService vlogClipService;

    public VlogClipController(VlogClipService vlogClipService) {
        this.vlogClipService = vlogClipService;
    }

    // TODO: API 엔드포인트 작성
}
