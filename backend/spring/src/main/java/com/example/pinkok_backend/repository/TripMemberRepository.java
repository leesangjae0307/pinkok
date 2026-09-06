package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripMemberRepository extends JpaRepository<TripMember, TripMember.TripMemberId> {

}
