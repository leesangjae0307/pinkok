package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.ItineraryItemCreateRequest;
import com.example.pinkok_backend.dto.ItineraryItemResponse;
import com.example.pinkok_backend.dto.ItineraryItemUpdateRequest;
import com.example.pinkok_backend.dto.ItineraryReorderRequest;
import com.example.pinkok_backend.entity.ItineraryDay;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.Place;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.repository.DiaryMediaRepository;
import com.example.pinkok_backend.repository.DiaryRepository;
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

    public static final String ADDED_BY_USER = "USER";
    public static final String ADDED_BY_AI = "AI";

    private final ItineraryItemRepository itineraryItemRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final TripRepository tripRepository;
    private final PlaceService placeService;
    private final TripAccessGuard tripAccessGuard;
    private final DiaryRepository diaryRepository;
    private final DiaryMediaRepository diaryMediaRepository;

    public ItineraryItemService(ItineraryItemRepository itineraryItemRepository,
                                ItineraryDayRepository itineraryDayRepository,
                                TripRepository tripRepository,
                                PlaceService placeService,
                                TripAccessGuard tripAccessGuard,
                                DiaryRepository diaryRepository,
                                DiaryMediaRepository diaryMediaRepository) {
        this.itineraryItemRepository = itineraryItemRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.tripRepository = tripRepository;
        this.placeService = placeService;
        this.tripAccessGuard = tripAccessGuard;
        this.diaryRepository = diaryRepository;
        this.diaryMediaRepository = diaryMediaRepository;
    }

    @Transactional
    public ItineraryItemResponse create(Long userId, ItineraryItemCreateRequest request) {
        return create(userId, request, ADDED_BY_USER);
    }

    /** @param addedBy USER(사용자가 직접) / AI(AI 추천을 수락해서) */
    @Transactional
    public ItineraryItemResponse create(Long userId, ItineraryItemCreateRequest request, String addedBy) {
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
        item.setAddedBy(addedBy);
        item.setCreatedAt(LocalDateTime.now());

        return ItineraryItemResponse.from(itineraryItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ItineraryItemResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        Map<Long, ItineraryItemResponse.Stats> stats = statsOf(tripId);
        return itineraryItemRepository.findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(tripId).stream()
                .map(item -> ItineraryItemResponse.from(item, stats.get(item.getId())))
                .toList();
    }

    @Transactional
    public ItineraryItemResponse update(Long itemId, Long userId, ItineraryItemUpdateRequest request) {
        ItineraryItem item = getItemOrThrow(itemId);
        tripAccessGuard.requireMember(item.getTrip().getId(), userId);
        Long oldDayId = dayIdOf(item);

        // dayId / plannedArrivalTime 은 "키를 보냈는지"로 판단한다. null 을 보냈으면 해제다.
        if (request.isDayIdPresent()) {
            if (request.getDayId() == null) {
                // 날짜 미정으로 되돌린다. 순서는 날짜 안에서만 의미가 있어서 같이 비운다.
                item.setDay(null);
                item.setVisitOrder(null);
            } else if (!request.getDayId().equals(dayIdOf(item))) {
                ItineraryDay newDay = getDayInTripOrThrow(request.getDayId(), item.getTrip().getId());
                // 순서 번호를 먼저 구한다. 핀을 먼저 옮기면 조회 직전에 DB 에 반영(flush)돼서 자기 자신까지 세어진다.
                int order = nextOrder(newDay.getId());
                item.setDay(newDay);
                item.setVisitOrder(order);
            }
        }
        if (request.isPlannedArrivalTimePresent()) item.setPlannedArrivalTime(request.getPlannedArrivalTime());
        if (request.getStayMinutes() != null) item.setStayMinutes(request.getStayMinutes());
        if (request.getTransportMode() != null) item.setTransportMode(request.getTransportMode());
        if (request.getMemo() != null) item.setMemo(request.getMemo());

        ItineraryItem saved = itineraryItemRepository.saveAndFlush(item);
        // 원래 있던 날짜에서 빠졌으면 그 날짜에 남은 핀들의 번호를 1 부터 다시 붙인다
        if (oldDayId != null && !oldDayId.equals(dayIdOf(saved))) {
            compactDay(oldDayId);
        }
        return ItineraryItemResponse.from(saved, statsOf(saved.getTrip().getId()).get(saved.getId()));
    }

    @Transactional
    public void delete(Long itemId, Long userId) {
        ItineraryItem item = getItemOrThrow(itemId);
        tripAccessGuard.requireMember(item.getTrip().getId(), userId);
        Long dayId = dayIdOf(item);
        itineraryItemRepository.delete(item);
        itineraryItemRepository.flush();
        compactDay(dayId);
    }

    /**
     * 핀이 빠진 날짜의 순서 번호를 1, 2, 3 … 으로 다시 붙인다.
     * 안 그러면 1번 핀을 빼고 나면 2, 3 이 남고, 다시 넣을 때 4번이 되어 "2부터 시작"하는 날이 생긴다.
     */
    private void compactDay(Long dayId) {
        if (dayId == null) {
            return;
        }
        List<ItineraryItem> items = itineraryItemRepository.findAllByDay_IdOrderByVisitOrderAsc(dayId);
        boolean alreadyCompact = true;
        for (int i = 0; i < items.size(); i++) {
            if (!Integer.valueOf(i + 1).equals(items.get(i).getVisitOrder())) {
                alreadyCompact = false;
                break;
            }
        }
        if (!alreadyCompact) {
            renumber(items);
        }
    }

    /**
     * 주어진 순서대로 1 부터 번호를 붙인다.
     * (day_id, visit_order) 가 유니크라서, 임시로 음수를 줬다가 최종값을 주는 2단계로 바꾼다
     * — 안 그러면 중간에 다른 핀과 순서 번호가 겹쳐서 제약조건에 걸릴 수 있다.
     */
    private void renumber(List<ItineraryItem> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setVisitOrder(-(i + 1));
        }
        itineraryItemRepository.saveAllAndFlush(ordered);

        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setVisitOrder(i + 1);
        }
        itineraryItemRepository.saveAllAndFlush(ordered);
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

        renumber(ids.stream().map(byId::get).toList());

        Map<Long, ItineraryItemResponse.Stats> stats = statsOf(day.getTrip().getId());
        return ids.stream().map(id -> ItineraryItemResponse.from(byId.get(id), stats.get(id))).toList();
    }

    /**
     * 여행 안 핀들의 기록 통계(별점 평균·기록 수·첫 사진 썸네일). 핀 개수와 상관없이 쿼리 2번으로 끝낸다.
     * 기록이 없는 핀은 맵에 없다 (호출한 쪽에서 {@code get} 이 null 이면 "기록 없음"으로 처리).
     */
    private Map<Long, ItineraryItemResponse.Stats> statsOf(Long tripId) {
        Map<Long, DiaryMediaRepository.ItemFirstMedia> firstMedia = diaryMediaRepository.firstMediaByTripId(tripId).stream()
                .collect(Collectors.toMap(DiaryMediaRepository.ItemFirstMedia::getItemId, m -> m, (a, b) -> a));

        Map<Long, ItineraryItemResponse.Stats> result = new java.util.HashMap<>();
        for (DiaryRepository.ItemDiaryStat s : diaryRepository.statsByTripId(tripId)) {
            Double avg = s.getAvgRating() == null ? null : Math.round(s.getAvgRating() * 10) / 10.0;
            result.put(s.getItemId(), new ItineraryItemResponse.Stats(
                    avg, s.getReviewCount().intValue(), thumbnailOf(firstMedia.get(s.getItemId()))));
        }
        return result;
    }

    /** 사진은 썸네일이 없으면 원본을, 영상은 썸네일(첫 장면)이 없으면 보여줄 게 없어서 null */
    private static String thumbnailOf(DiaryMediaRepository.ItemFirstMedia media) {
        if (media == null) {
            return null;
        }
        if (media.getThumbnailUrl() != null) {
            return media.getThumbnailUrl();
        }
        return "IMAGE".equals(media.getMediaType()) ? media.getFileUrl() : null;
    }

    private int nextOrder(Long dayId) {
        return itineraryItemRepository.findMaxVisitOrder(dayId) + 1;
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
