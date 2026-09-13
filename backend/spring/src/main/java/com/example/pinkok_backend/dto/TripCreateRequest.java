package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TripCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 100)
    private String region;

    private LocalDate startDate;

    private LocalDate endDate;

    /** SOLO / COUPLE / FAMILY / FRIEND */
    private String companionType;

    private Boolean isPublic;
}
