package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {

    List<ItineraryDay> findAllByTrip_IdOrderByDayNumberAsc(Long tripId);

    boolean existsByTrip_IdAndDayNumber(Long tripId, Integer dayNumber);

}
