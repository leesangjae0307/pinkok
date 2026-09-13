package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.ItineraryItemCreateRequest;
import com.example.pinkok_backend.dto.ItineraryItemResponse;
import com.example.pinkok_backend.dto.ItineraryItemUpdateRequest;
import com.example.pinkok_backend.dto.ItineraryReorderRequest;
import com.example.pinkok_backend.entity.ItineraryDay;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.Place;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.repository.ItineraryDayRepository;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItineraryItemService {

    private static final String ADDED_BY_USER = "USER";

    private final ItineraryItemRepository itineraryItemRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final TripRepository tripRepository;
    private final PlaceService placeService;
    private final TripAccessGuard tripAccessGuard;

    public ItineraryItemService(ItineraryItemRepository itineraryItemRepository,
                                ItineraryDayRepository itineraryDayRepository,
                                TripRepository tripRepository,
                                PlaceService placeService,
                                TripAccessGuard tripAccessGuard) {
        this.itineraryItemRepository = itineraryItemRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.tripRepository = tripRepository;
        this.placeService = placeService;
        this.tripAccessGuard = tripAccessGuard;
    }

    @Transactional
    public ItineraryItemResponse create(Long userId, ItineraryItemCreateRequest request) {
        tripAccessGuard.requireMember(request.getTripId(), userId);

        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(request.getTripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));

        ItineraryDay day = request.getDayId() == null ? null : getDayInTripOrThrow(request.getDayId(), trip.getId());
        Place place = placeService.findOrCreate(request.getPlace());

        ItineraryItem item = new ItineraryItem();
        item.setTrip(trip);
        item.setDay(day);
        item.setPlace(place);
        item.setVisitOrder(day == null ? null : nextOrder(day.getId()));
        item.setPlannedArrivalTime(request.getPlannedArrivalTime());
        item.setStayMinutes(request.getStayMinutes());
        item.setTransportMode(request.getTransportMode());
        item.setMemo(request.getMemo());
        item.setAddedBy(ADDED_BY_USER);
        item.setCreatedAt(LocalDateTime.now());

        return ItineraryItemResponse.from(itineraryItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ItineraryItemResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        return itineraryItemRepository.findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(tripId).stream()
                .map(ItineraryItemResponse::from)
                .toList();
    }

    @Transactional
    public ItineraryItemResponse update(Long itemId, Long userId, ItineraryItemUpdateRequest request) {
        ItineraryItem item = getItemOrThrow(itemId);
        tripAccessGuard.requireMember(item.getTrip().getId(), userId);

        if (request.getDayId() != null && !request.getDayId().equals(dayIdOf(item))) {
            ItineraryDay newDay = getDayInTripOrThrow(request.getDayId(), item.getTrip().getId());
            item.setDay(newDay);
            item.setVisitOrder(nextOrder(newDay.getId()));
        }
        if (request.getPlannedArrivalTime() != null) item.setPlannedArrivalTime(request.getPlannedArrivalTime());
        if (request.getStayMinutes() != null) item.setStayMinutes(request.getStayMinutes());
        if (request.getTransportMode() != null) item.setTransportMode(request.getTransportMode());
        if (request.getMemo() != null) item.setMemo(request.getMemo());

        return ItineraryItemResponse.from(itineraryItemRepository.save(item));
    }

    @Transactional
    public void delete(Long itemId, Long userId) {
        ItineraryItem item = getItemOrThrow(itemId);
        tripAccessGuard.requireMember(item.getTrip().getId(), userId);
        itineraryItemRepository.delete(item);
    }

    /**
     * 한 날짜 안의 핀 순서를 통째로 다시 매긴다.
     * (day_id, visit_order) 가 유니크라서, 임시로 음수를 줬다가 최종값을 주는 2단계로 바꾼다
     * — 안 그러면 중간에 다른 핀과 순서 번호가 겹쳐서 제약조건에 걸릴 수 있다.
     */
    @Transactional
    public List<ItineraryItemResponse> reorder(Long userId, ItineraryReorderRequest request) {
        ItineraryDay day = itineraryDayRepository.findById(request.getDayId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일자를 찾을 수 없습니다."));
        tripAccessGuard.requireMember(day.getTrip().getId(), userId);

        List<ItineraryItem> items = itineraryItemRepository.findAllByDay_IdOrderByVisitOrderAsc(day.getId());
        Map<Long, ItineraryItem> byId = items.stream().collect(Collectors.toMap(ItineraryItem::getId, i -> i));

        List<Long> ids = request.getItemIds();
        if (ids.size() != items.size() || !byId.keySet().containsAll(ids)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemIds 가 이 날짜의 핀 목록과 정확히 일치해야 합니다.");
        }

        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setVisitOrder(-(i + 1));
        }
        itineraryItemRepository.saveAllAndFlush(items);

        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setVisitOrder(i + 1);
        }
        itineraryItemRepository.saveAllAndFlush(items);

        return ids.stream().map(id -> ItineraryItemResponse.from(byId.get(id))).toList();
    }

    private int nextOrder(Long dayId) {
        return itineraryItemRepository.countByDay_Id(dayId) + 1;
    }

    private Long dayIdOf(ItineraryItem item) {
        return item.getDay() == null ? null : item.getDay().getId();
    }

    private ItineraryDay getDayInTripOrThrow(Long dayId, Long tripId) {
        ItineraryDay day = itineraryDayRepository.findById(dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일자를 찾을 수 없습니다."));
        if (!day.getTrip().getId().equals(tripId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 여행의 일자입니다.");
        }
        return day;
    }

    private ItineraryItem getItemOrThrow(Long itemId) {
        return itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "핀을 찾을 수 없습니다."));
    }
}
