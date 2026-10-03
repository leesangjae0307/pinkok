package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.RouteOptimizationRequest;
import com.example.pinkok_backend.dto.RouteOptimizationResponse;
import com.example.pinkok_backend.dto.RouteOptimizationResult;
import com.example.pinkok_backend.entity.ItineraryDay;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.RouteOptimization;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.repository.ItineraryDayRepository;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.RouteOptimizationRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.route.RouteOptimizer;
import com.example.pinkok_backend.route.RouteOptimizer.Point;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 동선 최적화. LLM 이 아니라 거리 계산 알고리즘({@link RouteOptimizer})이라 동기로 바로 결과를 준다.
 * 제안(POST)과 반영(apply)이 분리돼 있어서, 사용자가 결과를 보고 마음에 들 때만 일정이 바뀐다.
 */
@Service
public class RouteOptimizationService {

    private final RouteOptimizationRepository routeOptimizationRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final TripRepository tripRepository;
    private final TripAccessGuard tripAccessGuard;
    private final ObjectMapper objectMapper;

    public RouteOptimizationService(RouteOptimizationRepository routeOptimizationRepository,
                                    ItineraryItemRepository itineraryItemRepository,
                                    ItineraryDayRepository itineraryDayRepository,
                                    TripRepository tripRepository,
                                    TripAccessGuard tripAccessGuard,
                                    ObjectMapper objectMapper) {
        this.routeOptimizationRepository = routeOptimizationRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.tripRepository = tripRepository;
        this.tripAccessGuard = tripAccessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RouteOptimizationResponse create(Long userId, RouteOptimizationRequest request) {
        tripAccessGuard.requireMember(request.getTripId(), userId);
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(request.getTripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));

        List<ItineraryDay> days = itineraryDayRepository.findAllByTrip_IdOrderByDayNumberAsc(trip.getId());
        List<ItineraryItem> items = itineraryItemRepository.findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(trip.getId());
        if (days.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일자가 없습니다. 먼저 여행 일자를 만들어 주세요.");
        }

        boolean redistribute = request.shouldRedistribute();
        Map<Long, ItineraryItem> itemById = new HashMap<>();
        items.forEach(i -> itemById.put(i.getId(), i));

        int beforeDistance = currentTotalDistanceM(items);
        List<RouteOptimizationResult.Day> resultDays = new ArrayList<>();
        List<Long> untouched = new ArrayList<>();

        if (redistribute) {
            if (items.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최적화할 핀이 없습니다.");
            }
            List<Point> path = RouteOptimizer.shortestPath(items.stream().map(RouteOptimizationService::toPoint).toList());
            List<List<Point>> chunks = RouteOptimizer.splitIntoDays(path, days.size());
            for (int d = 0; d < chunks.size(); d++) {
                resultDays.add(buildDay(days.get(d), chunks.get(d), itemById));
            }
        } else {
            for (ItineraryDay day : days) {
                List<Point> dayPoints = items.stream()
                        .filter(i -> i.getDay() != null && i.getDay().getId().equals(day.getId()))
                        .map(RouteOptimizationService::toPoint).toList();
                if (!dayPoints.isEmpty()) {
                    resultDays.add(buildDay(day, RouteOptimizer.shortestPath(dayPoints), itemById));
                }
            }
            items.stream().filter(i -> i.getDay() == null).forEach(i -> untouched.add(i.getId()));
            if (resultDays.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "날짜에 배정된 핀이 없습니다.");
            }
        }

        RouteOptimizationResult result = new RouteOptimizationResult(redistribute, beforeDistance, resultDays, untouched);

        RouteOptimization entity = new RouteOptimization();
        entity.setTrip(trip);
        entity.setTotalDistanceM(resultDays.stream().mapToInt(RouteOptimizationResult.Day::totalDistanceM).sum());
        entity.setTotalDurationMin(resultDays.stream().mapToInt(RouteOptimizationResult.Day::totalDurationMin).sum());
        entity.setResultJson(toJson(result));
        entity.setIsApplied(false);
        entity.setCreatedAt(LocalDateTime.now());
        return RouteOptimizationResponse.of(routeOptimizationRepository.save(entity), result);
    }

    @Transactional(readOnly = true)
    public List<RouteOptimizationResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        return routeOptimizationRepository.findAllByTrip_IdOrderByCreatedAtDescIdDesc(tripId).stream()
                .map(e -> RouteOptimizationResponse.of(e, fromJson(e.getResultJson())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteOptimizationResponse get(Long id, Long userId) {
        RouteOptimization entity = getOrThrow(id, userId);
        return RouteOptimizationResponse.of(entity, fromJson(entity.getResultJson()));
    }

    /**
     * 제안을 일정에 반영한다: 핀의 날짜·순서·이동수단을 바꾼다.
     * 제안을 만든 뒤 핀이 추가·삭제·이동됐으면 오래된 제안이므로 409 (다시 만들어야 한다).
     */
    @Transactional
    public RouteOptimizationResponse apply(Long id, Long userId) {
        RouteOptimization entity = getOrThrow(id, userId);
        if (Boolean.TRUE.equals(entity.getIsApplied())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 일정에 반영된 제안입니다.");
        }
        RouteOptimizationResult result = fromJson(entity.getResultJson());
        Long tripId = entity.getTrip().getId();

        List<ItineraryItem> items = itineraryItemRepository.findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(tripId);
        Map<Long, ItineraryItem> itemById = new HashMap<>();
        items.forEach(i -> itemById.put(i.getId(), i));

        Set<Long> planned = new HashSet<>(result.untouchedItemIds());
        result.days().forEach(d -> d.stops().forEach(s -> planned.add(s.itemId())));
        if (!planned.equals(itemById.keySet())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제안을 만든 뒤 핀이 바뀌었습니다. 다시 최적화해 주세요.");
        }
        if (!result.redistribute()) {
            for (RouteOptimizationResult.Day d : result.days()) {
                for (RouteOptimizationResult.Stop s : d.stops()) {
                    ItineraryItem item = itemById.get(s.itemId());
                    if (item.getDay() == null || !item.getDay().getId().equals(d.dayId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "제안을 만든 뒤 핀의 날짜가 바뀌었습니다. 다시 최적화해 주세요.");
                    }
                }
            }
        }

        Map<Long, ItineraryDay> dayById = new HashMap<>();
        itineraryDayRepository.findAllByTrip_IdOrderByDayNumberAsc(tripId).forEach(d -> dayById.put(d.getId(), d));

        // (day_id, visit_order) 유니크 제약 때문에 임시 음수 순서를 먼저 준다 (ItineraryItemService.reorder 와 같은 방식)
        List<ItineraryItem> touched = new ArrayList<>();
        int temp = 1;
        for (RouteOptimizationResult.Day d : result.days()) {
            for (RouteOptimizationResult.Stop s : d.stops()) {
                ItineraryItem item = itemById.get(s.itemId());
                item.setVisitOrder(-(temp++));
                touched.add(item);
            }
        }
        itineraryItemRepository.saveAllAndFlush(touched);

        for (RouteOptimizationResult.Day d : result.days()) {
            ItineraryDay day = dayById.get(d.dayId());
            if (day == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "제안을 만든 뒤 일자가 삭제됐습니다. 다시 최적화해 주세요.");
            }
            for (RouteOptimizationResult.Stop s : d.stops()) {
                ItineraryItem item = itemById.get(s.itemId());
                item.setDay(day);
                item.setVisitOrder(s.order());
                item.setTransportMode(s.transportMode());
            }
        }
        itineraryItemRepository.saveAllAndFlush(touched);

        entity.setIsApplied(true);
        return RouteOptimizationResponse.of(routeOptimizationRepository.save(entity), result);
    }

    private RouteOptimizationResult.Day buildDay(ItineraryDay day, List<Point> orderedPoints, Map<Long, ItineraryItem> itemById) {
        List<RouteOptimizationResult.Stop> stops = new ArrayList<>();
        int totalDistance = 0;
        int totalDuration = 0;
        for (int i = 0; i < orderedPoints.size(); i++) {
            Point p = orderedPoints.get(i);
            ItineraryItem item = itemById.get(p.id());
            int distance = 0;
            int duration = 0;
            String mode = null;
            if (i > 0) {
                distance = RouteOptimizer.roadDistanceM(orderedPoints.get(i - 1), p);
                mode = RouteOptimizer.transportModeFor(distance);
                duration = RouteOptimizer.durationMin(distance, mode);
            }
            totalDistance += distance;
            totalDuration += duration;
            stops.add(new RouteOptimizationResult.Stop(item.getId(), item.getPlace().getId(), item.getPlace().getName(),
                    i + 1, mode, distance, duration));
        }
        return new RouteOptimizationResult.Day(day.getId(), day.getDayNumber(), totalDistance, totalDuration, stops);
    }

    /** 지금 일정(날짜별 현재 순서) 그대로 이동할 때의 총 거리. 날짜 미배정 핀은 제외. */
    private int currentTotalDistanceM(List<ItineraryItem> items) {
        Map<Long, List<Point>> byDay = new java.util.LinkedHashMap<>();
        for (ItineraryItem item : items) { // 날짜·순서대로 정렬돼 들어온다
            if (item.getDay() != null) {
                byDay.computeIfAbsent(item.getDay().getId(), k -> new ArrayList<>()).add(toPoint(item));
            }
        }
        return byDay.values().stream().mapToInt(RouteOptimizer::pathLengthM).sum();
    }

    private static Point toPoint(ItineraryItem item) {
        return new Point(item.getId(), item.getPlace().getLatitude().doubleValue(), item.getPlace().getLongitude().doubleValue());
    }

    private RouteOptimization getOrThrow(Long id, Long userId) {
        RouteOptimization entity = routeOptimizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "동선 최적화 결과를 찾을 수 없습니다."));
        tripAccessGuard.requireMember(entity.getTrip().getId(), userId);
        return entity;
    }

    private String toJson(RouteOptimizationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JacksonException e) {
            throw new IllegalStateException("동선 결과를 JSON 으로 변환하지 못했습니다.", e);
        }
    }

    private RouteOptimizationResult fromJson(String json) {
        try {
            return objectMapper.readValue(json, RouteOptimizationResult.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("저장된 동선 결과를 읽지 못했습니다.", e);
        }
    }
}
