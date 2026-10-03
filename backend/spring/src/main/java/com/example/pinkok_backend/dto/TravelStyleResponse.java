package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.TravelStyle;

public record TravelStyleResponse(Long id, String code, String name) {

    public static TravelStyleResponse from(TravelStyle style) {
        return new TravelStyleResponse(style.getId(), style.getCode(), style.getName());
    }
}
