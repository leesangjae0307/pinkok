package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.TravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TravelStyleRepository extends JpaRepository<TravelStyle, Long> {

    List<TravelStyle> findAllByCodeIn(Collection<String> codes);

}
