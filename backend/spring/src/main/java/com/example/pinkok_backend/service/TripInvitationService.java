package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TripInvitationResponse;
import com.example.pinkok_backend.dto.UserSearchResponse;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripInvitation;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.TripInvitationRepository;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 여행 초대: 아이디로 팀원을 지목해 초대하고, 받은 사람이 수락해야 실제 팀원이 된다.
 *
 * <p><b>초대장(trip_invitations)과 참여자(trip_members)는 다른 테이블이다.</b>
 * 초대는 "물어본 기록"이고, 수락했을 때만 참여자가 된다. 지금까지 만든 모든 여행 하위 API가
 * 참여자 테이블로 권한을 확인하므로(TripAccessGuard), 거절했거나 아직 답하지 않은 사람은
 * 남의 여행을 볼 수 없다.
 *
 * <p>초대는 닉네임이 아니라 <b>아이디(username)</b>로 지목한다. 닉네임은 중복될 수 있어서
 * 누구인지 특정할 수 없기 때문이다. 검색은 닉네임으로도 되지만, 고른 뒤 보내는 값은 아이디다.
 */
@Service
public class TripInvitationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String ROLE_MEMBER = "MEMBER";

    /** 한 번에 초대할 수 있는 인원 (여행 하나에 우르르 보내는 것 방지) */
    private static final int MAX_INVITEES_AT_ONCE = 10;
    private static final int SEARCH_LIMIT = 20;
    private static final int SEARCH_MIN_KEYWORD = 2;

    private final TripInvitationRepository tripInvitationRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripAccessGuard tripAccessGuard;
    private final NotificationService notificationService;

    public TripInvitationService(TripInvitationRepository tripInvitationRepository,
                                 TripMemberRepository tripMemberRepository,
                                 TripRepository tripRepository,
                                 UserRepository userRepository,
                                 TripAccessGuard tripAccessGuard,
                                 NotificationService notificationService) {
        this.tripInvitationRepository = tripInvitationRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripAccessGuard = tripAccessGuard;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------
    // 초대할 사람 찾기
    // ------------------------------------------------------------------

    /**
     * 아이디나 닉네임으로 사용자를 찾는다. 자기 자신은 빼고 돌려준다.
     *
     * @param tripId 있으면 이미 팀원이거나 초대 중인 사람을 표시해준다 (앱에서 "추가됨"으로 보여주라고)
     */
    @Transactional(readOnly = true)
    public List<UserSearchResponse> search(Long userId, String keyword, Long tripId) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.length() < SEARCH_MIN_KEYWORD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "검색어를 " + SEARCH_MIN_KEYWORD + "글자 이상 입력해주세요.");
        }
        if (tripId != null) {
            tripAccessGuard.requireMember(tripId, userId);
        }

        return userRepository.search(trimmed, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .filter(found -> !found.getId().equals(userId))
                .map(found -> UserSearchResponse.of(found,
                        tripId != null && tripMemberRepository.existsByTrip_IdAndUser_Id(tripId, found.getId()),
                        tripId != null && isPending(tripId, found.getId())))
                .toList();
    }

    // ------------------------------------------------------------------
    // 초대 보내기
    // ------------------------------------------------------------------

    /** 여행을 만든 뒤에 한 명을 더 초대한다. */
    @Transactional
    public TripInvitationResponse invite(Long userId, Long tripId, String username) {
        tripAccessGuard.requireOwner(tripId, userId);

        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));
        User inviter = getUserOrThrow(userId);
        User invitee = findByUsernameOrThrow(username);

        return TripInvitationResponse.from(createInvitation(trip, inviter, invitee));
    }

    /**
     * 여행을 만들 때 받은 아이디 목록으로 한꺼번에 초대한다.
     * 여행 생성 흐름에서 불리므로, 이미 같은 트랜잭션 안이다.
     */
    @Transactional
    public List<TripInvitationResponse> inviteAll(Trip trip, User inviter, List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }

        // 같은 아이디를 두 번 적어 보내도 한 번만 처리한다
        Set<String> unique = new LinkedHashSet<>();
        for (String username : usernames) {
            if (StringUtils.hasText(username)) {
                unique.add(username.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (unique.size() > MAX_INVITEES_AT_ONCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "한 번에 " + MAX_INVITEES_AT_ONCE + "명까지 초대할 수 있습니다.");
        }

        List<TripInvitationResponse> created = new ArrayList<>();
        for (String username : unique) {
            created.add(TripInvitationResponse.from(
                    createInvitation(trip, inviter, findByUsernameOrThrow(username))));
        }
        return created;
    }

    private TripInvitation createInvitation(Trip trip, User inviter, User invitee) {
        if (invitee.getId().equals(inviter.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신은 초대할 수 없습니다.");
        }
        if (tripMemberRepository.existsByTrip_IdAndUser_Id(trip.getId(), invitee.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "@" + invitee.getUsername() + " 님은 이미 이 여행의 팀원입니다.");
        }
        if (isPending(trip.getId(), invitee.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "@" + invitee.getUsername() + " 님에게 이미 초대를 보냈습니다. 답을 기다리는 중입니다.");
        }

        TripInvitation invitation = new TripInvitation();
        invitation.setTrip(trip);
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setStatus(STATUS_PENDING);
        invitation.setCreatedAt(LocalDateTime.now());
        tripInvitationRepository.save(invitation);

        notificationService.notify(invitee,
                NotificationService.TYPE_TRIP_INVITED,
                "'" + trip.getTitle() + "' 여행에 초대받았어요",
                "'" + inviter.getNickname() + "'님이 함께 가자고 했어요.",
                NotificationService.TARGET_INVITATION,
                invitation.getId());

        return invitation;
    }

    // ------------------------------------------------------------------
    // 받은 초대 · 수락 · 거절
    // ------------------------------------------------------------------

    /** 내가 받은 초대. 기본은 아직 답하지 않은 것만. */
    @Transactional(readOnly = true)
    public List<TripInvitationResponse> received(Long userId, boolean includeResponded) {
        List<String> statuses = includeResponded
                ? List.of(STATUS_PENDING, STATUS_ACCEPTED, STATUS_REJECTED)
                : List.of(STATUS_PENDING);

        return tripInvitationRepository.findAllByInvitee(userId, statuses).stream()
                .map(TripInvitationResponse::from)
                .toList();
    }

    /** 한 여행의 초대 현황 (누구를 불렀고 누가 답했는지). 팀원이면 볼 수 있다. */
    @Transactional(readOnly = true)
    public List<TripInvitationResponse> ofTrip(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        return tripInvitationRepository.findAllByTripId(tripId).stream()
                .map(TripInvitationResponse::from)
                .toList();
    }

    /**
     * 수락하면 비로소 이 여행의 팀원이 된다. 그때부터 일정·기록·PinLog 를 볼 수 있다.
     */
    @Transactional
    public TripInvitationResponse accept(Long invitationId, Long userId) {
        TripInvitation invitation = getPendingOrThrow(invitationId, userId);
        Trip trip = invitation.getTrip();

        if (trip.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "이미 삭제된 여행입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        // 초대를 받아둔 사이에 다른 경로로 팀원이 됐을 수도 있다 — 그때는 조용히 넘어간다
        if (!tripMemberRepository.existsByTrip_IdAndUser_Id(trip.getId(), userId)) {
            TripMember member = new TripMember();
            member.setId(new TripMember.TripMemberId());
            member.getId().setTripId(trip.getId());
            member.getId().setUserId(userId);
            member.setTrip(trip);
            member.setUser(invitation.getInvitee());
            member.setRole(ROLE_MEMBER);
            member.setJoinedAt(now);
            tripMemberRepository.save(member);
        }

        invitation.setStatus(STATUS_ACCEPTED);
        invitation.setRespondedAt(now);
        tripInvitationRepository.save(invitation);

        return TripInvitationResponse.from(invitation);
    }

    /** 거절하면 상태만 바뀌고 팀원은 되지 않는다. 같은 사람을 나중에 다시 초대할 수는 있다. */
    @Transactional
    public TripInvitationResponse reject(Long invitationId, Long userId) {
        TripInvitation invitation = getPendingOrThrow(invitationId, userId);

        invitation.setStatus(STATUS_REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        tripInvitationRepository.save(invitation);

        return TripInvitationResponse.from(invitation);
    }

    // ------------------------------------------------------------------
    // 잡일
    // ------------------------------------------------------------------

    private boolean isPending(Long tripId, Long inviteeId) {
        return tripInvitationRepository.existsByTrip_IdAndInvitee_IdAndStatus(tripId, inviteeId, STATUS_PENDING);
    }

    private TripInvitation getPendingOrThrow(Long invitationId, Long userId) {
        TripInvitation invitation = tripInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."));

        if (invitation.getInvitee() == null || !invitation.getInvitee().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "나에게 온 초대만 응답할 수 있습니다.");
        }
        if (!STATUS_PENDING.equals(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 응답한 초대입니다.");
        }
        return invitation;
    }

    private User findByUsernameOrThrow(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "@" + username.trim() + " 아이디를 가진 사용자가 없습니다."));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
