package com.example.pinkok_backend.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PlaceSearchPageResponse {

    private final List<PlaceSearchResponse> places;
    /** 다음 페이지(page+1)가 더 있는지. */
    private final boolean hasMore;

    public PlaceSearchPageResponse(List<PlaceSearchResponse> places, boolean hasMore) {
        this.places = places;
        this.hasMore = hasMore;
    }
}
