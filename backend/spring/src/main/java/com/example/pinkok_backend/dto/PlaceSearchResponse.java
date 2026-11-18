package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.kakao.KakaoAddressSearchResponse;
import com.example.pinkok_backend.kakao.KakaoKeywordSearchResponse;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 카카오맵 검색 결과 한 건. {@link PlaceInput} 과 필드가 같아서, 사용자가 목록에서
 * 하나를 고르면 그 값을 그대로 {@code POST /itinerary-items} 의 {@code place} 에 넣으면 된다.
 *
 * <p>{@code type} 이 {@code ADDRESS} 면 카카오맵에 등록된 장소가 아니라 <b>주소만 찾은 결과</b>다
 * (에어비앤비 숙소처럼 등록 안 된 곳). 이름은 건물명이나 주소로 임시로 채워져 있으므로,
 * 앱이 사용자에게 장소 이름을 입력받아 {@code name} 을 바꿔서 추가한다.
 */
@Getter
public class PlaceSearchResponse {

    /** 카카오맵에 등록된 장소 */
    public static final String TYPE_PLACE = "PLACE";
    /** 주소로만 찾은 곳 (이름은 사용자가 입력) */
    public static final String TYPE_ADDRESS = "ADDRESS";

    /** PLACE / ADDRESS */
    private final String type;
    private final String kakaoPlaceId;
    private final String name;
    private final String roadAddress;
    private final String lotAddress;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String categoryGroupCode;
    private final String categoryName;
    private final String phone;
    private final String placeUrl;

    private PlaceSearchResponse(KakaoKeywordSearchResponse.Document doc) {
        this.type = TYPE_PLACE;
        this.kakaoPlaceId = doc.getId();
        this.name = doc.getPlaceName();
        this.roadAddress = doc.getRoadAddressName();
        this.lotAddress = doc.getAddressName();
        this.latitude = new BigDecimal(doc.getY());
        this.longitude = new BigDecimal(doc.getX());
        this.categoryGroupCode = doc.getCategoryGroupCode();
        this.categoryName = doc.getCategoryName();
        this.phone = doc.getPhone();
        this.placeUrl = doc.getPlaceUrl();
    }

    private PlaceSearchResponse(KakaoAddressSearchResponse.Document doc) {
        String road = doc.getRoadAddress() == null ? null : blankToNull(doc.getRoadAddress().getAddressName());
        String lot = doc.getAddress() == null ? null : blankToNull(doc.getAddress().getAddressName());
        String building = doc.getRoadAddress() == null ? null : blankToNull(doc.getRoadAddress().getBuildingName());

        this.type = TYPE_ADDRESS;
        this.kakaoPlaceId = null;
        // 임시 이름: 건물명이 있으면 건물명, 없으면 주소. 앱이 사용자 입력으로 바꾼다
        this.name = building != null ? building : firstNonBlank(road, lot, doc.getAddressName());
        this.roadAddress = road;
        this.lotAddress = lot != null ? lot : (road == null ? blankToNull(doc.getAddressName()) : null);
        this.latitude = new BigDecimal(doc.getY());
        this.longitude = new BigDecimal(doc.getX());
        this.categoryGroupCode = null;
        this.categoryName = null;
        this.phone = null;
        this.placeUrl = null;
    }

    public static PlaceSearchResponse from(KakaoKeywordSearchResponse.Document doc) {
        return new PlaceSearchResponse(doc);
    }

    public static PlaceSearchResponse fromAddress(KakaoAddressSearchResponse.Document doc) {
        return new PlaceSearchResponse(doc);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
