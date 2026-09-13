package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ItineraryDayCreateRequest {

    @NotNull
    private Long tripId;

    /** 1일차, 2일차 ... */
    @NotNull
    @Min(1)
    private Integer dayNumber;

    private LocalDate visitDate;
}
