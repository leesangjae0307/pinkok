package com.example.pinkok_backend.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 카카오 Local API "키워드로 장소 검색" 응답 그대로 매핑 (snake_case). */
@Getter
@Setter
public class KakaoKeywordSearchResponse {

    private List<Document> documents;
    private Meta meta;

    @Getter
    @Setter
    public static class Document {

        private String id;

        @JsonProperty("place_name")
        private String placeName;

        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("road_address_name")
        private String roadAddressName;

        @JsonProperty("category_group_code")
        private String categoryGroupCode;

        @JsonProperty("category_name")
        private String categoryName;

        private String phone;

        @JsonProperty("place_url")
        private String placeUrl;

        /** 경도 (longitude) - 카카오는 x/y 로 준다. */
        private String x;

        /** 위도 (latitude) */
        private String y;
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
