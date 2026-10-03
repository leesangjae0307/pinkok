package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteOptimizationRequest {

    @NotNull(message = "tripId 는 필수입니다.")
    private Long tripId;

    /**
     * true(기본): 모든 핀(날짜 미배정 포함)을 일자들에 새로 나눠 담고 순서도 정한다.
     * false: 지금 배정된 날짜는 그대로 두고 각 날짜 안의 순서만 정한다 (날짜 미배정 핀은 건드리지 않음).
     */
    private Boolean redistribute;

    public boolean shouldRedistribute() {
        return redistribute == null || redistribute;
    }
}
