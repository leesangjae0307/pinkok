package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    List<ItineraryItem> findAllByDay_Id(Long dayId);

    List<ItineraryItem> findAllByDay_IdOrderByVisitOrderAsc(Long dayId);

    List<ItineraryItem> findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(Long tripId);

    int countByDay_Id(Long dayId);

    /** 같은 장소가 이 여행에 이미 꽂혀 있는지 (AI 후보를 중복으로 추가하지 않기 위해). */
    boolean existsByTrip_IdAndPlace_Id(Long tripId, Long placeId);

}
