package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /** 최신 배치가 먼저, 같은 배치 안에서는 모델이 준 순서(id 오름차순) 그대로 - 앞에 둔 게 더 추천하는 곳이다. */
    List<Recommendation> findAllByTrip_IdOrderByCreatedAtDescIdAsc(Long tripId);

    /** 이미 추천했던(수락·거절 포함) 장소명 - 같은 걸 또 추천하지 않게 프롬프트에 넣는다. */
    @Query("select r.suggestedName from Recommendation r where r.trip.id = :tripId")
    List<String> findSuggestedNamesByTripId(@Param("tripId") Long tripId);

}
