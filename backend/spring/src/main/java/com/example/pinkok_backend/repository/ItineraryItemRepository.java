package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    List<ItineraryItem> findAllByDay_Id(Long dayId);

    List<ItineraryItem> findAllByDay_IdOrderByVisitOrderAsc(Long dayId);

    List<ItineraryItem> findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(Long tripId);

    int countByDay_Id(Long dayId);

    /** 이 여행 일정에 이미 있는 장소명. 추천에서 제외할 때 쓴다 (lazy 로딩 없이 문자열만). */
    @Query("select i.place.name from ItineraryItem i where i.trip.id = :tripId")
    List<String> findPlaceNamesByTripId(@Param("tripId") Long tripId);

    /** 같은 장소가 이 여행에 이미 꽂혀 있는지 (AI 후보를 중복으로 추가하지 않기 위해). */
    boolean existsByTrip_IdAndPlace_Id(Long tripId, Long placeId);

}
