package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

}
