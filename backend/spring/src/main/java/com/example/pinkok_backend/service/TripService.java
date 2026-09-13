package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TripCreateRequest;
import com.example.pinkok_backend.dto.TripResponse;
import com.example.pinkok_backend.dto.TripUpdateRequest;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TripService {

    private static final String OWNER = "OWNER";

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final TripAccessGuard tripAccessGuard;

    public TripService(TripRepository tripRepository,
                       TripMemberRepository tripMemberRepository,
                       UserRepository userRepository,
                       TripAccessGuard tripAccessGuard) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.userRepository = userRepository;
        this.tripAccessGuard = tripAccessGuard;
    }

    /** 여행을 만들면 만든 사람이 자동으로 OWNER 로 등록된다. */
    @Transactional
    public TripResponse create(Long userId, TripCreateRequest request) {
        User user = getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setTitle(request.getTitle());
        trip.setRegion(request.getRegion());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setCompanionType(request.getCompanionType());
        trip.setStatus("PLANNING");
        trip.setIsPublic(request.getIsPublic() != null && request.getIsPublic());
        trip.setCreatedAt(now);
        trip.setUpdatedAt(now);
        trip = tripRepository.save(trip);

        TripMember owner = new TripMember();
        owner.setId(new TripMember.TripMemberId());
        owner.getId().setTripId(trip.getId());
        owner.getId().setUserId(userId);
        owner.setTrip(trip);
        owner.setUser(user);
        owner.setRole(OWNER);
        owner.setJoinedAt(now);
        tripMemberRepository.save(owner);

        return TripResponse.of(trip, OWNER);
    }

    /** 내가 팀원으로 속한 여행 목록 (만든 것 + 초대받아 들어간 것 전부). */
    @Transactional(readOnly = true)
    public List<TripResponse> listMine(Long userId) {
        return tripMemberRepository.findAllByUser_IdOrderByJoinedAtDesc(userId).stream()
                .filter(member -> member.getTrip().getDeletedAt() == null)
                .map(member -> TripResponse.of(member.getTrip(), member.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse get(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        Trip trip = getTripOrThrow(tripId);
        String role = tripMemberRepository.findByTrip_IdAndUser_Id(tripId, userId)
                .map(TripMember::getRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "이 여행에 접근할 권한이 없습니다."));
        return TripResponse.of(trip, role);
    }

    /** 여행 정보 수정은 OWNER 만 할 수 있다. */
    @Transactional
    public TripResponse update(Long tripId, Long userId, TripUpdateRequest request) {
        tripAccessGuard.requireOwner(tripId, userId);
        Trip trip = getTripOrThrow(tripId);

        if (request.getTitle() != null) trip.setTitle(request.getTitle());
        if (request.getRegion() != null) trip.setRegion(request.getRegion());
        if (request.getStartDate() != null) trip.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) trip.setEndDate(request.getEndDate());
        if (request.getCompanionType() != null) trip.setCompanionType(request.getCompanionType());
        if (request.getStatus() != null) trip.setStatus(request.getStatus());
        if (request.getCoverImageUrl() != null) trip.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getIsPublic() != null) trip.setIsPublic(request.getIsPublic());
        trip.setUpdatedAt(LocalDateTime.now());

        return TripResponse.of(tripRepository.save(trip), OWNER);
    }

    /** 삭제는 OWNER 만, 소프트 삭제(deleted_at) 로 처리한다. */
    @Transactional
    public void delete(Long tripId, Long userId) {
        tripAccessGuard.requireOwner(tripId, userId);
        Trip trip = getTripOrThrow(tripId);
        trip.setDeletedAt(LocalDateTime.now());
        tripRepository.save(trip);
    }

    private Trip getTripOrThrow(Long tripId) {
        return tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
