package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findAllByTrip_IdOrderByCreatedAtDescIdDesc(Long tripId);

    /** 이미 추천했던(수락·거절 포함) 장소명 - 같은 걸 또 추천하지 않게 프롬프트에 넣는다. */
    @Query("select r.suggestedName from Recommendation r where r.trip.id = :tripId")
    List<String> findSuggestedNamesByTripId(@Param("tripId") Long tripId);

}
