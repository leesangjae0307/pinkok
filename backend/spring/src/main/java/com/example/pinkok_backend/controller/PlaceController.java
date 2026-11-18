package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.category.PlaceCategory;
import com.example.pinkok_backend.dto.PlaceCategoryResponse;
import com.example.pinkok_backend.dto.PlaceSearchPageResponse;
import com.example.pinkok_backend.service.PlaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    /**
     * 카카오맵 키워드 장소 검색. 로그인한 사용자면 누구나 사용 가능.
     * 고른 결과를 그대로 {@code POST /itinerary-items} 의 {@code place} 필드에 넣으면 된다.
     */
    @GetMapping("/search")
    public PlaceSearchPageResponse search(@RequestParam String keyword,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return placeService.search(keyword, page, size);
    }

    /**
     * 장소 종류 9개 (보여줄 순서대로). 여행 "전체" 탭의 묶음 제목·순서와,
     * 카카오맵에 없는 장소를 직접 추가할 때 고르는 종류 목록에 쓴다.
     */
    @GetMapping("/categories")
    public List<PlaceCategoryResponse> categories() {
        return Arrays.stream(PlaceCategory.values()).map(PlaceCategoryResponse::from).toList();
    }
}
