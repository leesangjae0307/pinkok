package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TripMemberResponse;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * 여행 참여자 관리: 목록, 내보내기(OWNER), 나가기(MEMBER).
 *
 * <p>참여자를 <b>추가</b>하는 것은 여행 초대(trip-invitations)가 한다. 내보내거나 나가도 그 사람이 남긴
 * 핀·기록은 지우지 않는다 (같이 만든 여행 자료라서).
 */
@Service
public class TripMemberService {

    private static final String OWNER = "OWNER";

    private final TripMemberRepository tripMemberRepository;
    private final TripAccessGuard tripAccessGuard;

    public TripMemberService(TripMemberRepository tripMemberRepository, TripAccessGuard tripAccessGuard) {
        this.tripMemberRepository = tripMemberRepository;
        this.tripAccessGuard = tripAccessGuard;
    }

    /** 팀원이면 누구나 볼 수 있다. OWNER 가 맨 위, 나머지는 들어온 순서. */
    @Transactional(readOnly = true)
    public List<TripMemberResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        return tripMemberRepository.findAllByTrip_Id(tripId).stream()
                .sorted(Comparator
                        .comparing((TripMember m) -> !OWNER.equals(m.getRole()))
                        .thenComparing(TripMember::getJoinedAt))
                .map(member -> TripMemberResponse.of(member, userId))
                .toList();
    }

    /** OWNER 가 다른 참여자를 내보낸다. OWNER 자신은 내보낼 수 없다. */
    @Transactional
    public void remove(Long tripId, Long targetUserId, Long requesterId) {
        tripAccessGuard.requireOwner(tripId, requesterId);
        TripMember target = tripMemberRepository.findByTrip_IdAndUser_Id(tripId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이 여행의 참여자가 아닙니다."));
        if (OWNER.equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "여행을 만든 사람은 내보낼 수 없습니다. 여행을 없애려면 여행 삭제를 쓰세요.");
        }
        tripMemberRepository.delete(target);
    }

    /** 참여자가 스스로 나간다. 여행을 만든 사람은 나갈 수 없다 (여행이 주인 없이 남기 때문). */
    @Transactional
    public void leave(Long tripId, Long userId) {
        TripMember me = tripMemberRepository.findByTrip_IdAndUser_Id(tripId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "이 여행에 접근할 권한이 없습니다."));
        if (OWNER.equals(me.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "여행을 만든 사람은 나갈 수 없습니다. 여행을 없애려면 여행 삭제를 쓰세요.");
        }
        tripMemberRepository.delete(me);
    }
}
