package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRequestRepository extends JpaRepository<AiRequest, Long> {

    List<AiRequest> findAllByUser_IdOrderByRequestedAtDesc(Long userId);

    List<AiRequest> findAllByUser_IdAndTrip_IdOrderByRequestedAtDesc(Long userId, Long tripId);

}
