package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Pinlog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PinlogRepository extends JpaRepository<Pinlog, Long> {

    @Query("""
            select p from Pinlog p
            join fetch p.user
            where p.trip.id = :tripId
            order by p.createdAt desc
            """)
    List<Pinlog> findAllByTripId(@Param("tripId") Long tripId);
}
