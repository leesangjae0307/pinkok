package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.TripCreateRequest;
import com.example.pinkok_backend.dto.TripOwnerTransferRequest;
import com.example.pinkok_backend.dto.TripResponse;
import com.example.pinkok_backend.dto.TripUpdateRequest;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /** 여행 카드 생성. 만든 사람은 자동으로 OWNER 로 등록된다. */
    @PostMapping
    public ResponseEntity<TripResponse> create(@CurrentUserId Long userId,
                                                @Valid @RequestBody TripCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.create(userId, request));
    }

    /** 내가 팀원으로 속한 여행 목록. */
    @GetMapping
    public List<TripResponse> list(@CurrentUserId Long userId) {
        return tripService.listMine(userId);
    }

    /** 여행 상세. 팀원만 조회 가능(403). */
    @GetMapping("/{tripId}")
    public TripResponse get(@CurrentUserId Long userId, @PathVariable Long tripId) {
        return tripService.get(tripId, userId);
    }

    /** 여행 정보 수정. OWNER 만 가능(403). */
    @PatchMapping("/{tripId}")
    public TripResponse update(@CurrentUserId Long userId,
                                @PathVariable Long tripId,
                                @Valid @RequestBody TripUpdateRequest request) {
        return tripService.update(tripId, userId, request);
    }

    /**
     * 여행 주인을 다른 팀원에게 넘긴다. OWNER 만 가능(403), 넘긴 사람은 일반 팀원이 된다.
     * 새 주인은 이미 그 여행의 팀원이어야 한다.
     */
    @PatchMapping("/{tripId}/owner")
    public TripResponse transferOwner(@CurrentUserId Long userId,
                                      @PathVariable Long tripId,
                                      @Valid @RequestBody TripOwnerTransferRequest request) {
        return tripService.transferOwner(tripId, userId, request.getNewOwnerUserId());
    }

    /** 여행 삭제(소프트 삭제). OWNER 만 가능(403). */
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long tripId) {
        tripService.delete(tripId, userId);
        return ResponseEntity.noContent().build();
    }
}
