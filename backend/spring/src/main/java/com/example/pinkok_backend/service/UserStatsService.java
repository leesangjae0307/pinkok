package com.example.pinkok_backend.service;

import com.example.pinkok_backend.category.PlaceCategory;
import com.example.pinkok_backend.category.PlaceCategoryClassifier;
import com.example.pinkok_backend.dto.UserStatsResponse;
import com.example.pinkok_backend.dto.UserStatsResponse.PlaceEntry;
import com.example.pinkok_backend.dto.UserStatsResponse.RegionEntry;
import com.example.pinkok_backend.dto.UserStatsResponse.TasteEntry;
import com.example.pinkok_backend.entity.Diary;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.Place;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.repository.DiaryRepository;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.TripMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 프로필 화면의 내 여행 집계.
 *
 * <p>예전에는 앱이 여행마다 일정·기록·PinLog 를 따로 불러서 직접 셌다 (여행 N개면 요청 4N번).
 * 여기서는 "내 여행 목록"을 먼저 한 번 구하고, 그 여행들의 핀·기록·테마를 각각 {@code in (...)} 으로
 * 한 번씩만 읽는다. 여행이 몇 개든 쿼리는 3번이다.
 */
@Service
public class UserStatsService {

    private final TripMemberRepository tripMemberRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final DiaryRepository diaryRepository;

    public UserStatsService(TripMemberRepository tripMemberRepository,
                            ItineraryItemRepository itineraryItemRepository,
                            DiaryRepository diaryRepository) {
        this.tripMemberRepository = tripMemberRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.diaryRepository = diaryRepository;
    }

    @Transactional(readOnly = true)
    public UserStatsResponse statsOf(Long userId) {
        List<Trip> trips = tripMemberRepository.findActiveTripsOf(userId);
        if (trips.isEmpty()) {
            return new UserStatsResponse(0, 0, 0, List.of(), List.of(), List.of(), List.of());
        }
        List<Long> tripIds = trips.stream().map(Trip::getId).toList();

        // 저장한 장소 = 내 여행 일정에 담긴 핀
        List<ItineraryItem> pins = itineraryItemRepository.findAllWithPlaceByTripIds(tripIds);
        List<PlaceEntry> saved = pins.stream()
                .map(item -> entryOf(item, item.getCreatedAt()))
                .toList();

        // 방문한 장소 = 내가 기록(다이어리)을 남긴 핀. 한 핀에 내 기록은 하나뿐이라 겹치지 않는다.
        List<PlaceEntry> visited = diaryRepository.findMineInTrips(userId, tripIds).stream()
                .map(diary -> entryOf(diary.getItineraryItem(), diary.getCreatedAt()))
                .toList();

        List<RegionEntry> regions = regionsOf(trips);
        List<TasteEntry> tastes = tastesOf(pins);

        return new UserStatsResponse(visited.size(), saved.size(), regions.size(), visited, saved, regions, tastes);
    }

    private PlaceEntry entryOf(ItineraryItem item, LocalDateTime recordedAt) {
        Place place = item.getPlace();
        Trip trip = item.getTrip();
        return new PlaceEntry(item.getId(), trip.getId(), trip.getTitle(), place.getName(),
                place.getRoadAddress(), place.getLotAddress(), place.getCategoryName(), recordedAt);
    }

    /** 지역 이름별 여행 수. 지역을 비워둔 여행은 세지 않는다. */
    private List<RegionEntry> regionsOf(List<Trip> trips) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Trip trip : trips) {
            if (StringUtils.hasText(trip.getRegion())) {
                counts.merge(trip.getRegion().trim(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .map(e -> new RegionEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(RegionEntry::tripCount).reversed()
                        .thenComparing(RegionEntry::name))
                .toList();
    }

    /**
     * 여행 취향 = 내가 저장한 핀들의 장소 종류 비율. 종류는 {@link PlaceCategoryClassifier} 가 정한다
     * (여행 "전체" 탭의 종류별 묶음과 같은 규칙). "기타"는 취향으로 보여줄 의미가 없어서 빼고,
     * 비율은 나머지끼리 다시 계산한다. 이름은 취향용 이름(숙소 → "숙박")을 쓴다.
     */
    private List<TasteEntry> tastesOf(List<ItineraryItem> pins) {
        Map<PlaceCategory, Integer> counts = new LinkedHashMap<>();
        for (ItineraryItem pin : pins) {
            PlaceCategory category = PlaceCategoryClassifier.classify(pin.getPlace());
            if (category != PlaceCategory.ETC) {
                counts.merge(category, 1, Integer::sum);
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        return counts.entrySet().stream()
                .map(e -> new TasteEntry(e.getKey().code(), e.getKey().tasteLabel(), e.getValue(),
                        (int) Math.round(e.getValue() * 100.0 / total)))
                .sorted(Comparator.comparingInt(TasteEntry::count).reversed()
                        .thenComparing(TasteEntry::code))
                .toList();
    }
}
