package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.ItineraryItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class ItineraryItemResponse {

    private final Long id;
    private final Long tripId;
    private final Long dayId;
    private final Integer dayNumber;
    private final PlaceResponse place;
    private final Integer visitOrder;
    private final LocalTime plannedArrivalTime;
    private final Integer stayMinutes;
    private final String transportMode;
    private final String memo;
    private final String addedBy;
    private final LocalDateTime createdAt;

    private ItineraryItemResponse(ItineraryItem item) {
        this.id = item.getId();
        this.tripId = item.getTrip().getId();
        this.dayId = item.getDay() == null ? null : item.getDay().getId();
        this.dayNumber = item.getDay() == null ? null : item.getDay().getDayNumber();
        this.place = PlaceResponse.from(item.getPlace());
        this.visitOrder = item.getVisitOrder();
        this.plannedArrivalTime = item.getPlannedArrivalTime();
        this.stayMinutes = item.getStayMinutes();
        this.transportMode = item.getTransportMode();
        this.memo = item.getMemo();
        this.addedBy = item.getAddedBy();
        this.createdAt = item.getCreatedAt();
    }

    public static ItineraryItemResponse from(ItineraryItem item) {
        return new ItineraryItemResponse(item);
    }
}
