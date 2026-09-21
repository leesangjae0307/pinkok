package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripMemberRepository extends JpaRepository<TripMember, TripMember.TripMemberId> {

    boolean existsByTrip_IdAndUser_Id(Long tripId, Long userId);

    Optional<TripMember> findByTrip_IdAndUser_Id(Long tripId, Long userId);

    List<TripMember> findAllByTrip_Id(Long tripId);

    List<TripMember> findAllByUser_IdOrderByJoinedAtDesc(Long userId);

}
