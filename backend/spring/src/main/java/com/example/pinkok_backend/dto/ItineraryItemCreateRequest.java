package com.example.pinkok_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class ItineraryItemCreateRequest {

    @NotNull
    private Long tripId;

    /** 아직 날짜를 정하지 않았으면 비워둔다("미배정 핀"). */
    private Long dayId;

    @NotNull
    @Valid
    private PlaceInput place;

    private LocalTime plannedArrivalTime;

    private Integer stayMinutes;

    /** CAR / WALK / BUS / TRAIN */
    private String transportMode;

    @Size(max = 500)
    private String memo;
}
