package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    List<ItineraryItem> findAllByDay_Id(Long dayId);

    List<ItineraryItem> findAllByDay_IdOrderByVisitOrderAsc(Long dayId);

    List<ItineraryItem> findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(Long tripId);

    int countByDay_Id(Long dayId);

}
