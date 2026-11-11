package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    List<ItineraryItem> findAllByDay_Id(Long dayId);

    List<ItineraryItem> findAllByDay_IdOrderByVisitOrderAsc(Long dayId);

    List<ItineraryItem> findAllByTrip_IdOrderByDay_DayNumberAscVisitOrderAsc(Long tripId);

    /** 여러 여행의 핀을 장소·여행과 함께 한 번에 (프로필 집계용). 최근에 담은 순. */
    @Query("""
            select i from ItineraryItem i
            join fetch i.place
            join fetch i.trip
            where i.trip.id in :tripIds
            order by i.createdAt desc
            """)
    List<ItineraryItem> findAllWithPlaceByTripIds(@Param("tripIds") Collection<Long> tripIds);

    /** 그 날짜의 마지막 순서 번호 (핀이 없으면 0). 중간 핀을 지워 번호가 비어 있어도 겹치지 않게 개수 대신 최댓값을 쓴다. */
    @Query("select coalesce(max(i.visitOrder), 0) from ItineraryItem i where i.day.id = :dayId")
    int findMaxVisitOrder(@Param("dayId") Long dayId);

    int countByTrip_Id(Long tripId);

    /** 여행 목록에서 쓰는 "핀 개수". 여행마다 따로 세지 않고 한 번에 묶어서 센다 (N+1 방지). */
    @Query("select i.trip.id as tripId, count(i) as cnt from ItineraryItem i where i.trip.id in :tripIds group by i.trip.id")
    List<TripPinCount> countByTripIds(@Param("tripIds") Collection<Long> tripIds);

    interface TripPinCount {
        Long getTripId();

        Long getCnt();
    }

    /** 이 여행 일정에 이미 있는 장소명. 추천에서 제외할 때 쓴다 (lazy 로딩 없이 문자열만). */
    @Query("select i.place.name from ItineraryItem i where i.trip.id = :tripId")
    List<String> findPlaceNamesByTripId(@Param("tripId") Long tripId);

    /** 같은 장소가 이 여행에 이미 꽂혀 있는지 (AI 후보를 중복으로 추가하지 않기 위해). */
    boolean existsByTrip_IdAndPlace_Id(Long tripId, Long placeId);

}
