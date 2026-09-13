package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.ItineraryDayCreateRequest;
import com.example.pinkok_backend.dto.ItineraryDayResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.ItineraryDayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/itinerary-days")
public class ItineraryDayController {

    private final ItineraryDayService itineraryDayService;

    public ItineraryDayController(ItineraryDayService itineraryDayService) {
        this.itineraryDayService = itineraryDayService;
    }

    @PostMapping
    public ResponseEntity<ItineraryDayResponse> create(@CurrentUserId Long userId,
                                                        @Valid @RequestBody ItineraryDayCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itineraryDayService.create(userId, request));
    }

    @GetMapping
    public List<ItineraryDayResponse> list(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return itineraryDayService.list(tripId, userId);
    }

    /** 일자를 지워도 그 핀들은 "날짜 미배정" 으로 남고 삭제되지 않는다. */
    @DeleteMapping("/{dayId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long dayId) {
        itineraryDayService.delete(dayId, userId);
        return ResponseEntity.noContent().build();
    }
}
