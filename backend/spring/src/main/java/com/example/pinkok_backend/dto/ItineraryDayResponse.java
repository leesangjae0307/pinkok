package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.ItineraryDay;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ItineraryDayResponse {

    private final Long id;
    private final Long tripId;
    private final Integer dayNumber;
    private final LocalDate visitDate;

    private ItineraryDayResponse(ItineraryDay day) {
        this.id = day.getId();
        this.tripId = day.getTrip().getId();
        this.dayNumber = day.getDayNumber();
        this.visitDate = day.getVisitDate();
    }

    public static ItineraryDayResponse from(ItineraryDay day) {
        return new ItineraryDayResponse(day);
    }
}
