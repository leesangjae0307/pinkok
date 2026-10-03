package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 체크리스트에서 고른 후보들을 여행 지도에 핀으로 추가한다. */
@Getter
@Setter
public class AddCandidatesToTripRequest {

    @NotNull
    private Long tripId;

    /** 사용자가 체크한 후보 ID들. 모두 같은 AI 요청에서 나온 것이어야 한다. */
    @NotEmpty
    private List<Long> candidateIds;
}
