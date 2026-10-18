package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TravelStyleResponse;
import com.example.pinkok_backend.entity.TravelStyle;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripTravelStyle;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.TripTravelStyleRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 여행 하나의 취향. AI 추천은 요청에 취향이 없으면 이것을 먼저 쓰고, 이것도 없으면 사용자 취향을 쓴다. */
@Service
public class TripTravelStyleService {

    private final TripTravelStyleRepository tripTravelStyleRepository;
    private final TripRepository tripRepository;
    private final TravelStyleService travelStyleService;
    private final TripAccessGuard tripAccessGuard;

    public TripTravelStyleService(TripTravelStyleRepository tripTravelStyleRepository,
                                  TripRepository tripRepository,
                                  TravelStyleService travelStyleService,
                                  TripAccessGuard tripAccessGuard) {
        this.tripTravelStyleRepository = tripTravelStyleRepository;
        this.tripRepository = tripRepository;
        this.travelStyleService = travelStyleService;
        this.tripAccessGuard = tripAccessGuard;
    }

    @Transactional(readOnly = true)
    public List<TravelStyleResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        return tripTravelStyleRepository.findAllByTrip_Id(tripId).stream()
                .map(link -> TravelStyleResponse.from(link.getTravelStyle()))
                .sorted(Comparator.comparing(TravelStyleResponse::id))
                .toList();
    }

    /** 통째로 교체. 빠진 것은 해제, 새로 생긴 것은 추가하고 그대로인 것은 건드리지 않는다. 팀원이면 누구나 바꿀 수 있다. */
    @Transactional
    public List<TravelStyleResponse> replace(Long userId, Long tripId, List<String> styleCodes) {
        tripAccessGuard.requireMember(tripId, userId);
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));
        List<TravelStyle> wanted = travelStyleService.resolve(styleCodes);
        Set<Long> wantedIds = wanted.stream().map(TravelStyle::getId).collect(Collectors.toSet());

        List<TripTravelStyle> current = tripTravelStyleRepository.findAllByTrip_Id(tripId);
        Set<Long> currentIds = current.stream().map(link -> link.getTravelStyle().getId()).collect(Collectors.toSet());

        tripTravelStyleRepository.deleteAll(current.stream()
                .filter(link -> !wantedIds.contains(link.getTravelStyle().getId()))
                .toList());

        for (TravelStyle style : wanted) {
            if (!currentIds.contains(style.getId())) {
                TripTravelStyle link = new TripTravelStyle();
                TripTravelStyle.TripTravelStyleId id = new TripTravelStyle.TripTravelStyleId();
                id.setTripId(tripId);
                id.setStyleId(style.getId());
                link.setId(id);
                link.setTrip(trip);
                link.setTravelStyle(style);
                tripTravelStyleRepository.save(link);
            }
        }
        return wanted.stream().map(TravelStyleResponse::from).sorted(Comparator.comparing(TravelStyleResponse::id)).toList();
    }
}
