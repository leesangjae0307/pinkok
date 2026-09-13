package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.kakao.KakaoKeywordSearchResponse;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 카카오맵 검색 결과 한 건. {@link PlaceInput} 과 필드가 같아서, 사용자가 목록에서
 * 하나를 고르면 그 값을 그대로 {@code POST /itinerary-items} 의 {@code place} 에 넣으면 된다.
 */
@Getter
public class PlaceSearchResponse {

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

    public static PlaceSearchResponse from(KakaoKeywordSearchResponse.Document doc) {
        return new PlaceSearchResponse(doc);
    }
}
