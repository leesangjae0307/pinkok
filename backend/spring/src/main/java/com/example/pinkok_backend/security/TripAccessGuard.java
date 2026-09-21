package com.example.pinkok_backend.security;

import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.repository.TripMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * "요청한 사람이 이 여행의 팀원인가"를 확인하는 공통 코드.
 *
 * <p>PLAN.md 위험 항목: "여행이 여러 사람 것이 되면서, 일정·기록·PinLog를 조회하는
 * 모든 API가 이 확인을 해야 한다. 한 군데라도 빠지면 남의 여행이 보인다." —
 * 그래서 여행 하위 리소스(일정, 핀, 기록 등)를 다루는 서비스는 전부 이걸 통해서만
 * 접근을 확인한다. 개별 서비스에서 직접 TripMemberRepository 를 호출하지 말 것.
 *
 * <h3>사용 예</h3>
 * <pre>{@code
 * public List<ItineraryDay> getDays(Long tripId, Long userId) {
 *     tripAccessGuard.requireMember(tripId, userId); // 팀원 아니면 403
 *     return itineraryDayRepository.findAllByTrip_Id(tripId);
 * }
 * }</pre>
 */
@Component
public class TripAccessGuard {

    private static final String NOT_A_MEMBER = "이 여행에 접근할 권한이 없습니다.";
    private static final String OWNER_ONLY = "여행을 만든 사람(OWNER)만 할 수 있습니다.";

    private final TripMemberRepository tripMemberRepository;

    public TripAccessGuard(TripMemberRepository tripMemberRepository) {
        this.tripMemberRepository = tripMemberRepository;
    }

    /** 조회 · 항목 추가처럼 팀원이면 누구나 할 수 있는 동작. 팀원이 아니면 403. */
    public void requireMember(Long tripId, Long userId) {
        if (!tripMemberRepository.existsByTrip_IdAndUser_Id(tripId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NOT_A_MEMBER);
        }
    }

    /** 여행 정보 수정 · 삭제 · 팀원 관리처럼 만든 사람만 할 수 있는 동작. */
    public void requireOwner(Long tripId, Long userId) {
        TripMember member = tripMemberRepository.findByTrip_IdAndUser_Id(tripId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, NOT_A_MEMBER));

        if (!"OWNER".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, OWNER_ONLY);
        }
    }
}
