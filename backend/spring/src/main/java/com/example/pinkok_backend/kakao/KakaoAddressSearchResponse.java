package com.example.pinkok_backend.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 카카오 Local API "주소 검색" 응답 그대로 매핑 (snake_case).
 * 도로명·지번 주소를 넣으면 그 주소의 좌표를 돌려준다. 카카오맵에 가게로 등록되지 않은 곳(에어비앤비 숙소 등)을
 * 주소로 찾을 때 쓴다.
 */
@Getter
@Setter
public class KakaoAddressSearchResponse {

    private List<Document> documents;
    private Meta meta;

    @Getter
    @Setter
    public static class Document {

        /** 전체 주소 (도로명 또는 지번, 검색한 형태에 따라) */
        @JsonProperty("address_name")
        private String addressName;

        /** 경도 (longitude) */
        private String x;

        /** 위도 (latitude) */
        private String y;

        /** 지번 주소 정보. 없을 수 있다 */
        private Address address;

        /** 도로명 주소 정보. 없을 수 있다 */
        @JsonProperty("road_address")
        private RoadAddress roadAddress;
    }

    @Getter
    @Setter
    public static class Address {

        @JsonProperty("address_name")
        private String addressName;
    }

    @Getter
    @Setter
    public static class RoadAddress {

        @JsonProperty("address_name")
        private String addressName;

        /** 건물 이름 (예: "OO빌라"). 없으면 빈 문자열 */
        @JsonProperty("building_name")
        private String buildingName;
    }

    @Getter
    @Setter
    public static class Meta {

        @JsonProperty("total_count")
        private int totalCount;

        @JsonProperty("is_end")
        private boolean isEnd;
    }
}
