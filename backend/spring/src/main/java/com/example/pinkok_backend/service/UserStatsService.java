package com.example.pinkok_backend.service;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * 여행 취향 = 내가 저장한 핀들의 장소 분류 비율. 분류는 맛집·카페·바다·자연·관광명소·문화시설·숙박·쇼핑·기타
     * 9개로 고정이다 ({@link #categoryOf}). 카테고리 정보가 아예 없는 핀(직접 찍은 장소 등)은 비율 계산에서 뺀다.
     */
    private List<TasteEntry> tastesOf(List<ItineraryItem> pins) {
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItineraryItem pin : pins) {
            Category category = categoryOf(pin.getPlace());
            if (category != null) {
                names.putIfAbsent(category.code(), category.name());
                counts.merge(category.code(), 1, Integer::sum);
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        return counts.entrySet().stream()
                .map(e -> new TasteEntry(e.getKey(), names.get(e.getKey()), e.getValue(),
                        (int) Math.round(e.getValue() * 100.0 / total)))
                .sorted(Comparator.comparingInt(TasteEntry::count).reversed()
                        .thenComparing(TasteEntry::code))
                .toList();
    }

    private record Category(String code, String name) {
    }

    // 취향 분류는 아래 9개로 고정한다. 앱이 아이콘·색을 이 코드들로만 고르면 되도록.
    private static final Category FOOD = new Category("FD6", "맛집");
    private static final Category CAFE = new Category("CE7", "카페");
    private static final Category SEA = new Category("SEA", "바다");
    private static final Category NATURE = new Category("NATURE", "자연");
    private static final Category SIGHT = new Category("AT4", "관광명소");
    private static final Category CULTURE = new Category("CT1", "문화시설");
    private static final Category STAY = new Category("AD5", "숙박");
    private static final Category SHOPPING = new Category("MT1", "쇼핑");
    private static final Category ETC = new Category("ETC", "기타");

    /** 카카오 큰 분류 코드 중 그대로 쓰는 것. 나머지(편의점·주차장·은행 …)는 기타로 묶는다. */
    private static final Map<String, Category> BY_GROUP_CODE = Map.of(
            "FD6", FOOD, "CE7", CAFE, "AT4", SIGHT, "CT1", CULTURE, "AD5", STAY, "MT1", SHOPPING);

    /** 이 코드가 붙은 장소는 이름에 "바다"가 들어가도 코드대로 둔다 (예: 바다뷰 카페는 카페). */
    private static final Set<String> KEEP_CODE = Set.of("FD6", "CE7", "AD5", "MT1");

    /**
     * 카카오 카테고리 경로의 단어로 바다·자연을 가려낸다. 단어가 이 말로 끝나면 해당한다.
     * 예: "여행 > 관광,명소 > 해수욕장" → 해수욕장 → 바다 / "여행 > 공원 > 도시근린공원" → 공원 → 자연
     */
    private static final List<String> SEA_WORDS = List.of("해수욕장", "해변", "해안", "바다", "항구", "포구", "등대");
    private static final List<String> NATURE_WORDS = List.of(
            "오름", "고개", "숲", "계곡", "폭포", "호수", "저수지", "공원", "수목원", "휴양림",
            "자연", "생태", "동굴", "습지", "정원", "들판", "평야");

    /** 한 글자 단어는 "부동산"처럼 다른 말 끝에 붙는 경우가 있어 단어가 정확히 같을 때만 인정한다. */
    private static final List<String> SEA_EXACT = List.of("섬");
    private static final List<String> NATURE_EXACT = List.of("산", "강");

    /**
     * 장소 하나를 9개 분류 중 하나로. 카테고리 정보가 아예 없으면 null (비율 계산에서 뺀다).
     *
     * <p>바다·자연을 코드보다 먼저 본다. 카카오는 해수욕장·오름에도 관광명소(AT4) 코드를 붙여서,
     * 코드부터 보면 전부 관광명소가 되기 때문이다. 단 음식점·카페·숙박·쇼핑 코드는 그대로 둔다.
     */
    private Category categoryOf(Place place) {
        String code = StringUtils.hasText(place.getCategoryGroupCode()) ? place.getCategoryGroupCode().trim() : null;
        String path = place.getCategoryName();
        if (code == null && !StringUtils.hasText(path)) {
            return null;
        }

        if (code == null || !KEEP_CODE.contains(code)) {
            List<String> words = wordsOf(path);
            if (endsWithAny(words, SEA_WORDS) || words.stream().anyMatch(SEA_EXACT::contains)) {
                return SEA;
            }
            if (endsWithAny(words, NATURE_WORDS) || words.stream().anyMatch(NATURE_EXACT::contains)) {
                return NATURE;
            }
        }
        if (code != null && BY_GROUP_CODE.containsKey(code)) {
            return BY_GROUP_CODE.get(code);
        }
        return ETC;
    }

    /** "여행 > 관광,명소 > 해수욕장" → [여행, 관광, 명소, 해수욕장] */
    private List<String> wordsOf(String path) {
        if (!StringUtils.hasText(path)) {
            return List.of();
        }
        return Arrays.stream(path.split("[>,]"))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .toList();
    }

    private boolean endsWithAny(List<String> words, List<String> suffixes) {
        return words.stream().anyMatch(word -> suffixes.stream().anyMatch(word::endsWith));
    }
}
