package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripMemberRepository extends JpaRepository<TripMember, TripMember.TripMemberId> {

    boolean existsByTrip_IdAndUser_Id(Long tripId, Long userId);

    Optional<TripMember> findByTrip_IdAndUser_Id(Long tripId, Long userId);

    List<TripMember> findAllByTrip_Id(Long tripId);

    List<TripMember> findAllByUser_IdOrderByJoinedAtDesc(Long userId);

    /** 내가 참여 중인 여행(삭제된 여행 제외)을 최근 참여순으로. 여행까지 한 번에 읽는다. */
    @Query("""
            select t from TripMember m join m.trip t
            where m.user.id = :userId and t.deletedAt is null
            order by m.joinedAt desc
            """)
    List<Trip> findActiveTripsOf(@Param("userId") Long userId);

}
