package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 핀을 추가할 때 같이 넘기는 장소 정보. 카카오맵 검색 결과를 그대로 넣거나
 * (kakaoPlaceId 채워짐), 검색 기능이 아직 없으면 직접 입력해도 된다.
 *
 * <p>kakaoPlaceId 가 이미 저장된 장소와 같으면 그 장소를 재사용하고,
 * 아니면 여기 담긴 정보로 새 장소를 만든다 (PlaceService.findOrCreate).
 */
@Getter
@Setter
public class PlaceInput {

    /** 카카오맵 장소 ID. 없으면(직접 입력) 매번 새 장소로 취급한다. */
    private String kakaoPlaceId;

    @NotBlank
    private String name;

    private String roadAddress;
    private String lotAddress;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    private String categoryGroupCode;
    private String categoryName;
    private String phone;
    private String placeUrl;
}
