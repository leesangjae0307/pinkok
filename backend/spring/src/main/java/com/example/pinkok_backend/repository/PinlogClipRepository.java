package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.PinlogClip;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
