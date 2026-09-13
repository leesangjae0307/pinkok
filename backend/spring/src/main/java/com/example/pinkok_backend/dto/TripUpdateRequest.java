package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 부분 수정. null 인 필드는 그대로 둔다. */
@Getter
@Setter
public class TripUpdateRequest {

    @Size(max = 100)
    private String title;

    @Size(max = 100)
    private String region;

    private LocalDate startDate;

    private LocalDate endDate;

    private String companionType;

    /** PLANNING / ONGOING / COMPLETED */
    private String status;

    private String coverImageUrl;

    private Boolean isPublic;
}
