package com.example.pinkok_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationAcceptRequest {

    /** 어느 일자에 넣을지. 비우면 "날짜 미배정" 핀으로 추가된다. */
    private Long dayId;
}
