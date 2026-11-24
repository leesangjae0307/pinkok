package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.PinlogClip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PinlogClipRepository extends JpaRepository<PinlogClip, Long> {

    /** 합성에 쓸 클립을 순서대로. 원본 미디어와 장소까지 같이 읽는다. */
    @Query("""
            select c from PinlogClip c
            join fetch c.diaryMedia m
            join fetch m.diary d
            join fetch d.user
            join fetch d.itineraryItem i
            join fetch i.place
            where c.pinlog.id = :pinlogId
            order by c.clipOrder asc
            """)
    List<PinlogClip> findAllByPinlogId(@Param("pinlogId") Long pinlogId);

    @Query("""
            select c from PinlogClip c
            join fetch c.diaryMedia m
            join fetch m.diary d
            join fetch d.user
            join fetch d.itineraryItem i
            join fetch i.place
            where c.pinlog.id in :pinlogIds
            order by c.pinlog.id asc, c.clipOrder asc
            """)
    List<PinlogClip> findAllByPinlogIdIn(@Param("pinlogIds") Collection<Long> pinlogIds);

    void deleteAllByPinlog_Id(Long pinlogId);

    /**
     * 지워지는 기록 첨부를 쓰던 클립 행을 지운다. 클립이 첨부를 외래키로 물고 있어서, 이걸 먼저 지우지 않으면
     * 그 첨부(기록·핀)를 지울 수 없다. 이미 만들어진 PinLog 영상 파일은 그대로 남는다.
     */
    @Modifying
    @Query("delete from PinlogClip c where c.diaryMedia.id in :mediaIds")
    int deleteAllByDiaryMediaIdIn(@Param("mediaIds") Collection<Long> mediaIds);
}
