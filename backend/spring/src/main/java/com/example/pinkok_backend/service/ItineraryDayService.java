package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.ItineraryDayCreateRequest;
import com.example.pinkok_backend.dto.ItineraryDayResponse;
import com.example.pinkok_backend.entity.ItineraryDay;
import com.example.pinkok_backend.entity.ItineraryItem;
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

@Service
public class ItineraryDayService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final TripRepository tripRepository;
    private final TripAccessGuard tripAccessGuard;

    public ItineraryDayService(ItineraryDayRepository itineraryDayRepository,
                               ItineraryItemRepository itineraryItemRepository,
                               TripRepository tripRepository,
                               TripAccessGuard tripAccessGuard) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.tripRepository = tripRepository;
        this.tripAccessGuard = tripAccessGuard;
    }

    @Transactional
    public ItineraryDayResponse create(Long userId, ItineraryDayCreateRequest request) {
        tripAccessGuard.requireMember(request.getTripId(), userId);

        if (itineraryDayRepository.existsByTrip_IdAndDayNumber(request.getTripId(), request.getDayNumber())) {
            throw new IllegalArgumentException("이미 있는 일차입니다.");
        }

        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(request.getTripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));

        ItineraryDay day = new ItineraryDay();
        day.setTrip(trip);
        day.setDayNumber(request.getDayNumber());
        day.setVisitDate(request.getVisitDate());
        day.setCreatedAt(LocalDateTime.now());

        return ItineraryDayResponse.from(itineraryDayRepository.save(day));
    }

    @Transactional(readOnly = true)
    public List<ItineraryDayResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        return itineraryDayRepository.findAllByTrip_IdOrderByDayNumberAsc(tripId).stream()
                .map(ItineraryDayResponse::from)
                .toList();
    }

    /** 일자를 지우면 그 날짜에 배정된 핀들은 삭제되지 않고 "날짜 미배정" 상태로 남는다. */
    @Transactional
    public void delete(Long dayId, Long userId) {
        ItineraryDay day = getDayOrThrow(dayId);
        tripAccessGuard.requireMember(day.getTrip().getId(), userId);

        List<ItineraryItem> items = itineraryItemRepository.findAllByDay_Id(dayId);
        for (ItineraryItem item : items) {
            item.setDay(null);
            item.setVisitOrder(null);
        }
        itineraryItemRepository.saveAll(items);

        itineraryDayRepository.delete(day);
    }

    private ItineraryDay getDayOrThrow(Long dayId) {
        return itineraryDayRepository.findById(dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일자를 찾을 수 없습니다."));
    }
}
