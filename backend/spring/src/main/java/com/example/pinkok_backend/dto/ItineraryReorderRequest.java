package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 한 날짜 안에서 핀 순서를 다시 정할 때. itemIds 를 원하는 순서대로 넣으면 1번부터 다시 매겨진다. */
@Getter
@Setter
public class ItineraryReorderRequest {

    @NotNull
    private Long dayId;

    @NotEmpty
    private List<Long> itemIds;
}
