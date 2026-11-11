package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.TripTravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripTravelStyleRepository extends JpaRepository<TripTravelStyle, TripTravelStyle.TripTravelStyleId> {

    List<TripTravelStyle> findAllByTrip_Id(Long tripId);

    /** 비동기 스레드처럼 트랜잭션 밖에서 쓸 때 lazy 로딩 없이 표시명만 바로 가져온다. */
    @Query("select s.name from TripTravelStyle t join t.travelStyle s where t.trip.id = :tripId order by s.id")
    List<String> findStyleNamesByTripId(@Param("tripId") Long tripId);

}
