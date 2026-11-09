package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Pinlog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PinlogRepository extends JpaRepository<Pinlog, Long> {

    @Query("""
            select p from Pinlog p
            join fetch p.user
            where p.trip.id = :tripId
            order by p.createdAt desc
            """)
    List<Pinlog> findAllByTripId(@Param("tripId") Long tripId);

    /**
     * 합성할 PinLog 를 여행·만든 사람과 함께 읽는다.
     * 합성은 트랜잭션 밖(별도 스레드)에서 돌아서, 나중에 여행 이름을 꺼내려 하면 DB 연결이 이미 닫혀 있다.
     */
    @Query("""
            select p from Pinlog p
            join fetch p.trip
            join fetch p.user
            where p.id = :id
            """)
    Optional<Pinlog> findWithTripAndUserById(@Param("id") Long id);

    /** 서버가 꺼지면서 끝내지 못한 PinLog 를 찾는다 (실패 알림에 여행 이름이 필요해 함께 읽는다). */
    @Query("""
            select p from Pinlog p
            join fetch p.trip
            join fetch p.user
            where p.status in :statuses and p.createdAt < :before
            """)
    List<Pinlog> findAllByStatusInAndCreatedAtBefore(@Param("statuses") Collection<String> statuses,
                                                     @Param("before") LocalDateTime before);
}
