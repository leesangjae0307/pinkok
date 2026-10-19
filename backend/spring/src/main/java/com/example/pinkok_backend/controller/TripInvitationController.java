package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.TripInvitationCreateRequest;
import com.example.pinkok_backend.dto.TripInvitationResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.TripInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 여행 초대. 아이디(@username)로 지목해 초대하고, 받은 사람이 수락해야 팀원이 된다.
 * 여행을 만들면서 함께 초대하려면 POST /trips 의 inviteeUsernames 를 쓰면 된다.
 */
@RestController
@RequestMapping("/trip-invitations")
public class TripInvitationController {

    private final TripInvitationService tripInvitationService;

    public TripInvitationController(TripInvitationService tripInvitationService) {
        this.tripInvitationService = tripInvitationService;
    }

    /** 여행을 만든 뒤에 팀원을 더 초대한다 (여행을 만든 사람만). */
    @PostMapping
    public ResponseEntity<TripInvitationResponse> invite(@CurrentUserId Long userId,
                                                         @Valid @RequestBody TripInvitationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripInvitationService.invite(userId, request.getTripId(), request.getUsername()));
    }

    /**
     * 내가 받은 초대. 기본은 아직 답하지 않은 것(PENDING)만 온다.
     * {@code includeResponded=true} 를 주면 수락·거절한 것까지 함께 볼 수 있다.
     */
    @GetMapping
    public List<TripInvitationResponse> received(@CurrentUserId Long userId,
                                                 @RequestParam(defaultValue = "false") boolean includeResponded) {
        return tripInvitationService.received(userId, includeResponded);
    }

    /** 한 여행의 초대 현황 — 누구를 불렀고 누가 답했는지 (팀원이면 볼 수 있다). */
    @GetMapping("/trip")
    public List<TripInvitationResponse> ofTrip(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return tripInvitationService.ofTrip(tripId, userId);
    }

    /** 수락 — 이때 비로소 팀원이 되고, 내 여행 목록에 그 카드가 나타난다. */
    @PostMapping("/{invitationId}/accept")
    public TripInvitationResponse accept(@CurrentUserId Long userId, @PathVariable Long invitationId) {
        return tripInvitationService.accept(invitationId, userId);
    }

    /** 거절 — 팀원이 되지 않는다. 나중에 다시 초대받을 수는 있다. */
    @PostMapping("/{invitationId}/reject")
    public TripInvitationResponse reject(@CurrentUserId Long userId, @PathVariable Long invitationId) {
        return tripInvitationService.reject(invitationId, userId);
    }
}
