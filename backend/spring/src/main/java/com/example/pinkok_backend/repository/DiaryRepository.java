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

    /**
     * 여행 안의 핀마다 기록 개수와 별점 평균. 별점을 안 매긴 기록은 평균에서 빠지고(avg 가 null 을 무시),
     * 기록 개수에는 포함된다. 핀이 여러 개여도 쿼리는 한 번이다.
     */
    @Query("""
            select i.id as itemId, count(d) as reviewCount, avg(d.rating) as avgRating
            from Diary d join d.itineraryItem i
            where i.trip.id = :tripId
            group by i.id
            """)
    List<ItemDiaryStat> statsByTripId(@Param("tripId") Long tripId);

    interface ItemDiaryStat {
        Long getItemId();

        Long getReviewCount();

        Double getAvgRating();
    }
}
