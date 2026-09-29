package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.DiaryMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DiaryMediaRepository extends JpaRepository<DiaryMedia, Long> {

    List<DiaryMedia> findAllByDiary_IdOrderByDisplayOrderAsc(Long diaryId);

    /** 기록 여러 개의 첨부를 한 번에 읽는다 (목록 조회에서 N+1 방지). */
    List<DiaryMedia> findAllByDiary_IdInOrderByDiary_IdAscDisplayOrderAsc(Collection<Long> diaryIds);

    void deleteAllByDiary_Id(Long diaryId);
}
