package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.DiaryCreateRequest;
import com.example.pinkok_backend.dto.DiaryResponse;
import com.example.pinkok_backend.dto.DiaryUpdateRequest;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.DiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 장소별 기록(메모 · 사진 · 영상).
 * 사진과 영상은 먼저 POST /files 로 올리고, 받은 주소를 media 에 담아 보낸다.
 */
@RestController
@RequestMapping("/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    /** 핀 하나에 기록을 남긴다. 같은 핀에 이미 내 기록이 있으면 409. */
    @PostMapping
    public ResponseEntity<DiaryResponse> create(@CurrentUserId Long userId,
                                                @Valid @RequestBody DiaryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diaryService.create(userId, request));
    }

    /**
     * 기록 목록. 둘 중 하나만 보낸다.
     * <ul>
     *   <li>{@code ?tripId=1} — 여행 전체 기록을 동선 순서로 (팀원 것 모두)</li>
     *   <li>{@code ?itineraryItemId=1} — 핀 하나에 달린 팀원들의 기록</li>
     * </ul>
     */
    @GetMapping
    public List<DiaryResponse> list(@CurrentUserId Long userId,
                                    @RequestParam(required = false) Long tripId,
                                    @RequestParam(required = false) Long itineraryItemId) {
        return diaryService.list(userId, tripId, itineraryItemId);
    }

    /** 내 기록 수정. media 를 보내면 첨부가 그 목록으로 통째로 바뀐다. */
    @PatchMapping("/{diaryId}")
    public DiaryResponse update(@CurrentUserId Long userId,
                                @PathVariable Long diaryId,
                                @Valid @RequestBody DiaryUpdateRequest request) {
        return diaryService.update(diaryId, userId, request);
    }

    /** 내 기록 삭제. 붙어 있던 사진·영상 파일도 같이 지워진다. */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long diaryId) {
        diaryService.delete(diaryId, userId);
        return ResponseEntity.noContent().build();
    }
}
