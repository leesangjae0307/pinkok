package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.ClipOptionResponse;
import com.example.pinkok_backend.dto.PinlogCreateRequest;
import com.example.pinkok_backend.dto.PinlogResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.PinlogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PinLog: 장소 기록의 영상·사진을 이어붙여 만드는 미니 브이로그.
 * 합성은 오래 걸리므로 만들기 요청은 바로 202로 끝나고, 상태를 다시 조회해서 확인한다.
 */
@RestController
@RequestMapping("/pinlogs")
public class PinlogController {

    private final PinlogService pinlogService;

    public PinlogController(PinlogService pinlogService) {
        this.pinlogService = pinlogService;
    }

    /**
     * 핀마다 고를 수 있는 클립 목록 (내 영상 · 팀원 영상).
     * {@code includePhotos=true} 를 주면 사진도 함께 돌려준다.
     */
    @GetMapping("/clip-options")
    public List<ClipOptionResponse> clipOptions(@CurrentUserId Long userId,
                                                @RequestParam Long tripId,
                                                @RequestParam(defaultValue = "false") boolean includePhotos) {
        return pinlogService.clipOptions(tripId, userId, includePhotos);
    }

    /**
     * PinLog 만들기. 고른 클립을 순서대로 보내고, 비워두면 서버가 동선 순서로 자동 선택한다.
     * 응답은 status=QUEUED 이고, 완성 여부는 GET /pinlogs/{id} 로 확인한다.
     */
    @PostMapping
    public ResponseEntity<PinlogResponse> create(@CurrentUserId Long userId,
                                                 @Valid @RequestBody PinlogCreateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pinlogService.create(userId, request));
    }

    /** 진행 상태 조회. status=DONE 이면 videoUrl 을 재생하면 된다. */
    @GetMapping("/{pinlogId}")
    public PinlogResponse get(@CurrentUserId Long userId, @PathVariable Long pinlogId) {
        return pinlogService.get(pinlogId, userId);
    }

    @GetMapping
    public List<PinlogResponse> list(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return pinlogService.list(tripId, userId);
    }

    /** 만든 사람만 삭제할 수 있다. 합성된 영상 파일도 같이 지워진다. */
    @DeleteMapping("/{pinlogId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long pinlogId) {
        pinlogService.delete(pinlogId, userId);
        return ResponseEntity.noContent().build();
    }
}
