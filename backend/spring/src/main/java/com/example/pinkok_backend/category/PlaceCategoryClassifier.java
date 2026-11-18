package com.example.pinkok_backend.category;

import com.example.pinkok_backend.entity.Place;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.pinkok_backend.category.PlaceCategory.CAFE;
import static com.example.pinkok_backend.category.PlaceCategory.CULTURE;
import static com.example.pinkok_backend.category.PlaceCategory.ETC;
import static com.example.pinkok_backend.category.PlaceCategory.FOOD;
import static com.example.pinkok_backend.category.PlaceCategory.NATURE;
import static com.example.pinkok_backend.category.PlaceCategory.SEA;
import static com.example.pinkok_backend.category.PlaceCategory.SHOPPING;
import static com.example.pinkok_backend.category.PlaceCategory.SIGHT;
import static com.example.pinkok_backend.category.PlaceCategory.STAY;

/**
 * 장소 하나를 {@link PlaceCategory} 9개 중 하나로 나누는 규칙. 입력은 장소에 저장된 두 값이다.
 * <ul>
 *   <li>{@code categoryGroupCode}: 카카오 큰 분류 코드(FD6·CE7 …) 또는 사용자가 고른 우리 코드(SEA·NATURE …)</li>
 *   <li>{@code categoryName}: 카카오 카테고리 경로 (예: "여행 > 관광,명소 > 해수욕장")</li>
 * </ul>
 *
 * <p>판단 순서
 * <ol>
 *   <li>사용자가 직접 고른 바다·자연·기타 코드 → 그대로</li>
 *   <li>맛집·카페·숙소·쇼핑 코드 → 그대로 (이름에 "바다"가 들어간 카페도 카페)</li>
 *   <li>코드는 없지만 경로가 음식점·카페·숙박·쇼핑 → 그대로</li>
 *   <li>경로에 바다·자연 단어 → 바다 / 자연 (카카오가 해수욕장·오름에도 관광명소 코드를 붙여서 코드보다 먼저 본다)</li>
 *   <li>관광명소·문화시설 코드 → 그대로</li>
 *   <li>코드가 없으면 경로 첫 단계 (여행→관광명소, 문화,예술→문화시설 …)</li>
 *   <li>그 밖 → 기타</li>
 * </ol>
 *
 * <p>DB 를 보지 않는 순수한 계산이라 어디서든(응답 DTO, 취향 집계) 바로 부를 수 있게 static 으로 둔다.
 */
public final class PlaceCategoryClassifier {

    private PlaceCategoryClassifier() {
    }

    /** 카카오 큰 분류 코드 중 그대로 쓰는 것. 나머지(편의점·주차장·은행 …)는 경로로 판단하거나 기타. */
    private static final Map<String, PlaceCategory> BY_GROUP_CODE = Map.of(
            "FD6", FOOD, "CE7", CAFE, "AT4", SIGHT, "CT1", CULTURE, "AD5", STAY, "MT1", SHOPPING);

    /** 카카오에 없어서 우리가 만든 코드. 사용자가 직접 고른 것이므로 다른 규칙보다 먼저 그대로 쓴다. */
    private static final Set<PlaceCategory> OWN_CODES = Set.of(SEA, NATURE, ETC);

    /** 이름에 "바다"가 들어가도 그대로 두는 종류 (바다뷰 카페는 카페, 바다횟집은 맛집) */
    private static final Set<PlaceCategory> KEEP = Set.of(FOOD, CAFE, STAY, SHOPPING);

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

    /** 쇼핑으로 볼 경로 단어 */
    private static final List<String> SHOPPING_WORDS = List.of("쇼핑", "시장", "백화점", "아울렛", "면세점");

    public static PlaceCategory classify(Place place) {
        return classify(place.getCategoryGroupCode(), place.getCategoryName());
    }

    /** 분류 정보가 아예 없으면(직접 추가하고 종류를 안 고른 장소 등) 기타. */
    public static PlaceCategory classify(String groupCode, String path) {
        String code = StringUtils.hasText(groupCode) ? groupCode.trim() : null;

        // 1) 사용자가 고른 우리 코드
        PlaceCategory own = PlaceCategory.ofCode(code).filter(OWN_CODES::contains).orElse(null);
        if (own != null) {
            return own;
        }

        // 2) 맛집·카페·숙소·쇼핑 코드
        PlaceCategory byCode = code == null ? null : BY_GROUP_CODE.get(code);
        if (byCode != null && KEEP.contains(byCode)) {
            return byCode;
        }

        // 3) 코드가 없지만 경로가 맛집·카페·숙소·쇼핑
        PlaceCategory byPath = fromPath(path);
        if (code == null && byPath != null && KEEP.contains(byPath)) {
            return byPath;
        }

        // 4) 바다·자연
        List<String> words = wordsOf(path);
        if (endsWithAny(words, SEA_WORDS) || words.stream().anyMatch(SEA_EXACT::contains)) {
            return SEA;
        }
        if (endsWithAny(words, NATURE_WORDS) || words.stream().anyMatch(NATURE_EXACT::contains)) {
            return NATURE;
        }

        // 5) 관광명소·문화시설 코드, 6) 경로 첫 단계, 7) 기타
        if (byCode != null) {
            return byCode;
        }
        if (byPath != null) {
            return byPath;
        }
        return ETC;
    }

    /**
     * 카카오 카테고리 경로의 첫 단계로 분류한다. 모르는 경로면 null.
     * 예: "여행 > 관광,명소" → 관광명소 / "문화,예술 > 문화시설 > 박물관" → 문화시설 / "음식점 > 카페 > 커피전문점" → 카페
     */
    private static PlaceCategory fromPath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        List<String> steps = Arrays.stream(path.split(">"))
                .map(String::trim)
                .filter(step -> !step.isEmpty())
                .toList();
        if (steps.isEmpty()) {
            return null;
        }
        String top = steps.get(0);
        String second = steps.size() > 1 ? steps.get(1) : "";

        if (top.equals("카페") || (top.equals("음식점") && second.equals("카페"))) {
            return CAFE;
        }
        if (top.equals("음식점")) {
            return FOOD;
        }
        if (top.equals("숙박")) {
            return STAY;
        }
        if (steps.stream().anyMatch(step -> SHOPPING_WORDS.stream().anyMatch(step::contains))) {
            return SHOPPING;
        }
        if (top.equals("여행")) {
            return SIGHT;
        }
        if (top.startsWith("문화")) {
            return CULTURE;
        }
        return null;
    }

    /** "여행 > 관광,명소 > 해수욕장" → [여행, 관광, 명소, 해수욕장] */
    private static List<String> wordsOf(String path) {
        if (!StringUtils.hasText(path)) {
            return List.of();
        }
        return Arrays.stream(path.split("[>,]"))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .toList();
    }

    private static boolean endsWithAny(List<String> words, List<String> suffixes) {
        return words.stream().anyMatch(word -> suffixes.stream().anyMatch(word::endsWith));
    }
}
