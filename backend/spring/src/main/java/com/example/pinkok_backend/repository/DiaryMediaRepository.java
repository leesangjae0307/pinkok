package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.DiaryMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DiaryMediaRepository extends JpaRepository<DiaryMedia, Long> {

    List<DiaryMedia> findAllByDiary_IdOrderByDisplayOrderAsc(Long diaryId);

    /** 기록 여러 개의 첨부를 한 번에 읽는다 (목록 조회에서 N+1 방지). */
    List<DiaryMedia> findAllByDiary_IdInOrderByDiary_IdAscDisplayOrderAsc(Collection<Long> diaryIds);

    /**
     * 여행 하나에 달린 모든 사진·영상을 동선 순서(일차 → 방문순서)로 읽는다.
     * 작성자와 핀(장소)까지 같이 가져온다 — PinLog 에서 "핀마다 누구 영상이 있는지" 보여주고
     * 아무것도 고르지 않았을 때 자동으로 클립을 뽑는 데 쓴다.
     */
    @Query("""
            select m from DiaryMedia m
            join fetch m.diary d
            join fetch d.user u
            left join fetch u.avatar
            join fetch d.itineraryItem i
            join fetch i.place
            left join fetch i.day dd
            where i.trip.id = :tripId
            order by dd.dayNumber asc nulls last, i.visitOrder asc nulls last,
                     i.id asc, m.displayOrder asc
            """)
    List<DiaryMedia> findAllByTripId(@Param("tripId") Long tripId);

    void deleteAllByDiary_Id(Long diaryId);
}
