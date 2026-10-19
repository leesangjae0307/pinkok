package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.TripMemberResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.TripMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trip-members")
public class TripMemberController {

    private final TripMemberService tripMemberService;

    public TripMemberController(TripMemberService tripMemberService) {
        this.tripMemberService = tripMemberService;
    }

    /** 이 여행의 참여자 목록 (팀원만). OWNER 가 맨 위. */
    @GetMapping
    public List<TripMemberResponse> list(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return tripMemberService.list(tripId, userId);
    }

    /** 내가 이 여행에서 나간다 (OWNER 는 불가). */
    @DeleteMapping("/me")
    public ResponseEntity<Void> leave(@CurrentUserId Long userId, @RequestParam Long tripId) {
        tripMemberService.leave(tripId, userId);
        return ResponseEntity.noContent().build();
    }

    /** OWNER 가 참여자를 내보낸다. */
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> remove(@CurrentUserId Long userId,
                                       @PathVariable Long targetUserId,
                                       @RequestParam Long tripId) {
        tripMemberService.remove(tripId, targetUserId, userId);
        return ResponseEntity.noContent().build();
    }
}
