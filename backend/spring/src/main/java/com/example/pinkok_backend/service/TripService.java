package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TripCreateRequest;
import com.example.pinkok_backend.dto.TripResponse;
import com.example.pinkok_backend.dto.TripUpdateRequest;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TripService {

    private static final String OWNER = "OWNER";
    private static final String MEMBER = "MEMBER";

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final TripAccessGuard tripAccessGuard;
    private final TripInvitationService tripInvitationService;
    private final ItineraryItemRepository itineraryItemRepository;

    public TripService(TripRepository tripRepository,
                       TripMemberRepository tripMemberRepository,
                       UserRepository userRepository,
                       TripAccessGuard tripAccessGuard,
                       TripInvitationService tripInvitationService,
                       ItineraryItemRepository itineraryItemRepository) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.userRepository = userRepository;
        this.tripAccessGuard = tripAccessGuard;
        this.tripInvitationService = tripInvitationService;
        this.itineraryItemRepository = itineraryItemRepository;
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

        // 같이 갈 사람을 적어 보냈으면 초대장을 보낸다. 수락해야 팀원이 되므로 지금은 아직 나 혼자다.
        tripInvitationService.inviteAll(trip, user, request.getInviteeUsernames());

        return TripResponse.of(trip, OWNER, 0); // 방금 만든 여행이라 핀이 없다
    }

    /** 내가 팀원으로 속한 여행 목록 (만든 것 + 초대받아 들어간 것 전부). */
    @Transactional(readOnly = true)
    public List<TripResponse> listMine(Long userId) {
        List<TripMember> memberships = tripMemberRepository.findAllByUser_IdOrderByJoinedAtDesc(userId).stream()
                .filter(member -> member.getTrip().getDeletedAt() == null)
                .toList();
        // 핀 개수는 여행마다 따로 세지 않고 한 번에 묶어서 센다 (N+1 방지)
        Map<Long, Integer> pinCounts = memberships.isEmpty() ? Map.of()
                : itineraryItemRepository.countByTripIds(memberships.stream().map(m -> m.getTrip().getId()).toList()).stream()
                        .collect(Collectors.toMap(ItineraryItemRepository.TripPinCount::getTripId, c -> c.getCnt().intValue()));
        return memberships.stream()
                .map(member -> TripResponse.of(member.getTrip(), member.getRole(),
                        pinCounts.getOrDefault(member.getTrip().getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse get(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        Trip trip = getTripOrThrow(tripId);
        String role = tripMemberRepository.findByTrip_IdAndUser_Id(tripId, userId)
                .map(TripMember::getRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "이 여행에 접근할 권한이 없습니다."));
        return TripResponse.of(trip, role, itineraryItemRepository.countByTrip_Id(tripId));
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

        return TripResponse.of(tripRepository.save(trip), OWNER, itineraryItemRepository.countByTrip_Id(tripId));
    }

    /**
     * 여행 주인을 다른 팀원에게 넘긴다. 넘긴 사람은 일반 팀원(MEMBER)이 된다.
     *
     * <p>주인만 할 수 있는 일(여행 정보 수정·삭제·팀원 초대/내보내기)이 있어서, 주인이 빠질 때
     * 누군가는 그 자리를 이어받아야 한다. 탈퇴할 때도 이 과정을 거친다.
     */
    @Transactional
    public TripResponse transferOwner(Long tripId, Long userId, Long newOwnerUserId) {
        tripAccessGuard.requireOwner(tripId, userId);
        Trip trip = getTripOrThrow(tripId);

        if (userId.equals(newOwnerUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 이 여행의 주인입니다.");
        }

        TripMember newOwner = tripMemberRepository.findByTrip_IdAndUser_Id(tripId, newOwnerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "이 여행의 팀원에게만 넘길 수 있습니다."));
        TripMember oldOwner = tripMemberRepository.findByTrip_IdAndUser_Id(tripId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "이 여행의 팀원이 아닙니다."));

        handOver(oldOwner, newOwner);
        trip.setUpdatedAt(LocalDateTime.now());
        tripRepository.save(trip);

        return TripResponse.of(trip, MEMBER, itineraryItemRepository.countByTrip_Id(tripId));
    }

    /** 주인 자리를 넘긴다. 주인이 둘이 되지 않도록 둘을 같이 바꾼다. */
    private void handOver(TripMember oldOwner, TripMember newOwner) {
        oldOwner.setRole(MEMBER);
        newOwner.setRole(OWNER);
        tripMemberRepository.save(oldOwner);
        tripMemberRepository.save(newOwner);
    }

    /**
     * 탈퇴할 때, 내가 주인인 여행들을 정리한다.
     *
     * <p>팀원이 남아 있으면 <b>닉네임 가나다 순으로 첫 번째 사람</b>에게 주인을 넘긴다.
     * 떠나는 사람에게 "주인을 직접 넘기고 오라"고 요구하면 번거롭고, 그렇다고 주인 없는 여행을
     * 남기면 아무도 그 여행을 수정·삭제할 수 없게 되기 때문이다.
     * 혼자 쓰던 여행은 넘길 사람이 없으므로 같이 삭제한다(소프트 삭제).
     *
     * @return 주인을 넘긴 여행 수와 삭제한 여행 수
     */
    @Transactional
    public OwnedTripCleanup cleanupOwnedTripsFor(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        int transferred = 0;
        int deleted = 0;

        for (TripMember ownership : tripMemberRepository.findAllByUser_IdOrderByJoinedAtDesc(userId)) {
            if (!OWNER.equals(ownership.getRole()) || ownership.getTrip().getDeletedAt() != null) {
                continue;
            }
            Trip trip = ownership.getTrip();

            TripMember successor = nextOwnerOf(trip.getId(), userId);
            if (successor == null) {
                trip.setDeletedAt(now);
                trip.setUpdatedAt(now);
                tripRepository.save(trip);
                deleted++;
            } else {
                handOver(ownership, successor);
                trip.setUpdatedAt(now);
                tripRepository.save(trip);
                transferred++;
            }
        }
        return new OwnedTripCleanup(transferred, deleted);
    }

    /** 닉네임 가나다 순으로 첫 번째 팀원. 아무도 없으면 null. */
    private TripMember nextOwnerOf(Long tripId, Long leavingUserId) {
        Collator korean = Collator.getInstance(Locale.KOREAN);
        return tripMemberRepository.findAllByTrip_Id(tripId).stream()
                .filter(member -> !member.getUser().getId().equals(leavingUserId))
                .min(Comparator
                        .comparing((TripMember member) -> member.getUser().getNickname(), korean)
                        .thenComparing(member -> member.getUser().getId()))
                .orElse(null);
    }

    /** 탈퇴 정리 결과. 앱에서 "여행 2개의 주인이 바뀌었습니다" 처럼 안내할 때 쓸 수 있다. */
    public record OwnedTripCleanup(int transferred, int deleted) {
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
