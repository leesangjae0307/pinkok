package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByIdAndDeletedAtIsNull(Long id);

}
