package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    /** 한 핀에 한 사람은 기록 하나만 쓸 수 있으므로, 만들기 전에 이미 썼는지 확인한다. */
    boolean existsByItineraryItem_IdAndUser_Id(Long itineraryItemId, Long userId);

    /**
     * 여행 하나에 달린 팀원 전체의 기록을 동선 순서대로 가져온다.
     *
     * <p>join fetch 로 작성자·핀·장소를 한 번에 같이 읽는다.
     * 안 그러면 기록 20개를 그릴 때 "작성자 조회" 쿼리가 20번 더 나간다(N+1 문제).
     */
    @Query("""
            select d from Diary d
            join fetch d.user u
            left join fetch u.avatar
            join fetch d.itineraryItem i
            join fetch i.place
            left join fetch i.day dd
            where i.trip.id = :tripId
            order by dd.dayNumber asc nulls last, i.visitOrder asc nulls last, d.createdAt asc
            """)
    List<Diary> findAllByTripId(@Param("tripId") Long tripId);

    /** 핀 하나에 달린 팀원들의 기록. */
    @Query("""
            select d from Diary d
            join fetch d.user u
            left join fetch u.avatar
            join fetch d.itineraryItem i
            join fetch i.place
            left join fetch i.day dd
            where i.id = :itineraryItemId
            order by d.createdAt asc
            """)
    List<Diary> findAllByItineraryItemId(@Param("itineraryItemId") Long itineraryItemId);
}
