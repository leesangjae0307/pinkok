package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

}
