package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/** 부분 수정. null 인 필드는 그대로 둔다. dayId 로 다른 날짜로 옮기면 그 날짜 맨 뒤로 재배치된다. */
@Getter
@Setter
public class ItineraryItemUpdateRequest {

    private Long dayId;
    private LocalTime plannedArrivalTime;
    private Integer stayMinutes;
    private String transportMode;

    @Size(max = 500)
    private String memo;
}
