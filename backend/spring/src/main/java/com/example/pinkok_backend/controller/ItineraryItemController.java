package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.ItineraryItemCreateRequest;
import com.example.pinkok_backend.dto.ItineraryItemResponse;
import com.example.pinkok_backend.dto.ItineraryItemUpdateRequest;
import com.example.pinkok_backend.dto.ItineraryReorderRequest;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.ItineraryItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/itinerary-items")
public class ItineraryItemController {

    private final ItineraryItemService itineraryItemService;

    public ItineraryItemController(ItineraryItemService itineraryItemService) {
        this.itineraryItemService = itineraryItemService;
    }

    /** 지도에 핀 추가. dayId 를 안 넣으면 "날짜 미배정" 핀이 된다. */
    @PostMapping
    public ResponseEntity<ItineraryItemResponse> create(@CurrentUserId Long userId,
                                                         @Valid @RequestBody ItineraryItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itineraryItemService.create(userId, request));
    }

    @GetMapping
    public List<ItineraryItemResponse> list(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return itineraryItemService.list(tripId, userId);
    }

    @PatchMapping("/{itemId}")
    public ItineraryItemResponse update(@CurrentUserId Long userId,
                                         @PathVariable Long itemId,
                                         @RequestBody ItineraryItemUpdateRequest request) {
        return itineraryItemService.update(itemId, userId, request);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long itemId) {
        itineraryItemService.delete(itemId, userId);
        return ResponseEntity.noContent().build();
    }

    /** 한 날짜 안의 핀 순서를 통째로 다시 정한다. */
    @PatchMapping("/order")
    public List<ItineraryItemResponse> reorder(@CurrentUserId Long userId,
                                                @Valid @RequestBody ItineraryReorderRequest request) {
        return itineraryItemService.reorder(userId, request);
    }
}
