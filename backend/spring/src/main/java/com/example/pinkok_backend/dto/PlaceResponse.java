package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.category.PlaceCategory;
import com.example.pinkok_backend.category.PlaceCategoryClassifier;
import com.example.pinkok_backend.entity.Place;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 장소. {@code category} 는 앱 전체에서 쓰는 장소 종류 9개 중 하나다 ({@link PlaceCategory}).
 * 여행 "전체" 탭에서 이 값으로 장소를 묶어 보여주면 된다. 순서는 {@code categoryOrder} 오름차순.
 */
@Getter
public class PlaceResponse {

    private final Long id;
    private final String name;
    private final String roadAddress;
    private final String lotAddress;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    /** 카카오 카테고리 경로 원문 (예: "음식점 > 카페 > 커피전문점"). 직접 추가한 장소는 없을 수 있다 */
    private final String categoryName;
    /** 장소 종류 코드: FD6 / CE7 / AD5 / AT4 / NATURE / SEA / CT1 / MT1 / ETC */
    private final String category;
    /** 장소 종류 이름 (맛집, 카페, 숙소 …). "전체" 탭 묶음 제목으로 쓴다 */
    private final String categoryLabel;
    /** "전체" 탭에서 묶음을 보여줄 순서 (0 = 맛집 … 8 = 기타) */
    private final int categoryOrder;
    private final String phone;
    private final String placeUrl;

    private PlaceResponse(Place place, PlaceCategory kind) {
        this.id = place.getId();
        this.name = place.getName();
        this.roadAddress = place.getRoadAddress();
        this.lotAddress = place.getLotAddress();
        this.latitude = place.getLatitude();
        this.longitude = place.getLongitude();
        this.categoryName = place.getCategoryName();
        this.category = kind.code();
        this.categoryLabel = kind.label();
        this.categoryOrder = kind.order();
        this.phone = place.getPhone();
        this.placeUrl = place.getPlaceUrl();
    }

    public static PlaceResponse from(Place place) {
        return new PlaceResponse(place, PlaceCategoryClassifier.classify(place));
    }

    /** 핀에서 종류를 바꿨으면 그 종류로 보여준다 (장소 자체는 그대로) */
    public static PlaceResponse from(Place place, String categoryOverride) {
        return new PlaceResponse(place, PlaceCategoryClassifier.classifyPin(place, categoryOverride));
    }
}
