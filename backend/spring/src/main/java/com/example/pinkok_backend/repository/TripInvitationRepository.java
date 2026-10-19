package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.TripInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TripInvitationRepository extends JpaRepository<TripInvitation, Long> {

    /** 같은 사람을 같은 여행에 두 번 초대하지 않기 위한 확인. */
    boolean existsByTrip_IdAndInvitee_IdAndStatus(Long tripId, Long inviteeId, String status);

    /** 내가 받은 초대. 여행·초대한 사람까지 같이 읽어온다(목록에 바로 보여줘야 해서). */
    @Query("""
            select i from TripInvitation i
            join fetch i.trip t
            join fetch i.inviter u
            left join fetch u.avatar
            where i.invitee.id = :userId and i.status in :statuses
            order by i.createdAt desc
            """)
    List<TripInvitation> findAllByInvitee(@Param("userId") Long userId,
                                          @Param("statuses") Collection<String> statuses);

    /** 한 여행에 보낸 초대들 (초대 현황 표시용). */
    @Query("""
            select i from TripInvitation i
            join fetch i.invitee u
            left join fetch u.avatar
            where i.trip.id = :tripId
            order by i.createdAt asc
            """)
    List<TripInvitation> findAllByTripId(@Param("tripId") Long tripId);
}
