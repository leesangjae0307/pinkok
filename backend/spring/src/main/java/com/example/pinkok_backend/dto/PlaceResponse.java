package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Place;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PlaceResponse {

    private final Long id;
    private final String name;
    private final String roadAddress;
    private final String lotAddress;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String categoryName;
    private final String phone;
    private final String placeUrl;

    private PlaceResponse(Place place) {
        this.id = place.getId();
        this.name = place.getName();
        this.roadAddress = place.getRoadAddress();
        this.lotAddress = place.getLotAddress();
        this.latitude = place.getLatitude();
        this.longitude = place.getLongitude();
        this.categoryName = place.getCategoryName();
        this.phone = place.getPhone();
        this.placeUrl = place.getPlaceUrl();
    }

    public static PlaceResponse from(Place place) {
        return new PlaceResponse(place);
    }
}
