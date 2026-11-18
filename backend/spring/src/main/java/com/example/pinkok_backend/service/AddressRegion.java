package com.example.pinkok_backend.service;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 주소에서 "지역 확인용" 시/군/구를 뽑는다. AI 가 준 장소를 카카오맵 결과와 맞출 때, 엉뚱한 지역의 같은 이름 가게가
 * 들어오지 않도록 이 값이 카카오 결과 주소에 들어 있는지 확인한다.
 *
 * <p><b>시·도 단계는 쓰지 않는다.</b> AI 는 "서울특별시 강남구 …", "서울시 강남구 …"처럼 쓰는데, 카카오는
 * "서울 강남구 …"로 줄여서 준다. 예전에는 앞에서부터 "시"로 끝나는 첫 단어("서울특별시")를 잡아서
 * 특별시·광역시 주소의 장소가 전부 지역 확인에서 떨어졌다 (제주는 "제주특별자치도"가 "도"로 끝나 우연히 동작했다).
 *
 * <p>규칙
 * <ol>
 *   <li>주소 앞쪽의 행정구역 부분만 본다. 도로명·동·읍·면·리나 숫자가 나오면 멈춘다
 *       ("강남역 1번 출구"의 "출구"처럼 "구"로 끝나는 일반 단어를 잡지 않기 위해).</li>
 *   <li>시·도 단계(서울특별시·부산광역시·경기도·서울시 …)는 건너뛰고, 남은 시/군/구 중 <b>가장 구체적인 것(마지막)</b>을 쓴다.
 *       예: "경기 수원시 팔달구" → 팔달구</li>
 *   <li>시/군/구가 없으면(세종 등) 시·도를 카카오가 쓰는 짧은 이름으로 쓴다. 예: "세종특별자치시 조치원읍" → 세종</li>
 * </ol>
 */
final class AddressRegion {

    private AddressRegion() {
    }

    /** 시·도의 긴 이름 → 카카오가 주소에 쓰는 짧은 이름 */
    private static final Map<String, String> PROVINCE_SHORT = Map.ofEntries(
            Map.entry("서울특별시", "서울"), Map.entry("서울시", "서울"),
            Map.entry("부산광역시", "부산"), Map.entry("부산시", "부산"),
            Map.entry("대구광역시", "대구"), Map.entry("대구시", "대구"),
            Map.entry("인천광역시", "인천"), Map.entry("인천시", "인천"),
            Map.entry("광주광역시", "광주"),
            Map.entry("대전광역시", "대전"), Map.entry("대전시", "대전"),
            Map.entry("울산광역시", "울산"), Map.entry("울산시", "울산"),
            Map.entry("세종특별자치시", "세종"), Map.entry("세종시", "세종"),
            Map.entry("경기도", "경기"),
            Map.entry("강원도", "강원"), Map.entry("강원특별자치도", "강원"),
            Map.entry("충청북도", "충북"), Map.entry("충청남도", "충남"),
            Map.entry("전라북도", "전북"), Map.entry("전북특별자치도", "전북"),
            Map.entry("전라남도", "전남"),
            Map.entry("경상북도", "경북"), Map.entry("경상남도", "경남"),
            Map.entry("제주도", "제주"), Map.entry("제주특별자치도", "제주")
    );

    /** 이미 짧은 이름으로 쓴 시·도 ("서울 강남구 …") */
    private static final List<String> PROVINCE_SHORT_NAMES = List.of(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주");

    /** "구"로 끝나지만 행정구역이 아닌 흔한 말 */
    private static final List<String> NOT_DISTRICT = List.of("출구", "입구", "정문", "후문");

    /** 이 말로 끝나면 행정구역(시/군/구) 부분이 끝난 것으로 본다 */
    private static final List<String> STOP_SUFFIXES = List.of("로", "길", "동", "읍", "면", "리", "가");

    /** 주소에서 지역 확인용 이름을 뽑는다. 알 수 없으면 null (그때는 이름이 확실히 비슷할 때만 매칭한다). */
    static String extract(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        String province = null;
        String district = null;

        for (String token : address.trim().split("\\s+")) {
            if (isProvince(token)) {
                province = PROVINCE_SHORT.getOrDefault(token, token);
                continue;
            }
            if (NOT_DISTRICT.contains(token)) {
                break;              // "강남역 출구"의 출구처럼 행정구역이 아닌 말이 나오면 멈춘다
            }
            if (token.length() >= 2 && (token.endsWith("시") || token.endsWith("군") || token.endsWith("구"))) {
                district = token;   // 뒤에 나오는 것이 더 구체적이다 (수원시 → 팔달구)
                continue;
            }
            if (token.chars().anyMatch(Character::isDigit) || STOP_SUFFIXES.stream().anyMatch(token::endsWith)) {
                break;              // 도로명·동·번지부터는 행정구역이 아니다
            }
        }
        return district != null ? district : province;
    }

    private static boolean isProvince(String token) {
        return PROVINCE_SHORT.containsKey(token)
                || PROVINCE_SHORT_NAMES.contains(token)
                || token.endsWith("특별시") || token.endsWith("광역시")
                || token.endsWith("특별자치시") || token.endsWith("특별자치도");
    }
}
