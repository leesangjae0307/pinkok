package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.PlaceCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PlaceCandidateRepository extends JpaRepository<PlaceCandidate, Long> {

    /** 후보 목록. 좌표를 붙인 장소와 출처 스크린샷을 같이 읽어온다 (N+1 방지). */
    @Query("""
            select c from PlaceCandidate c
            left join fetch c.place
            left join fetch c.sourceImage
            where c.aiRequest.id = :aiRequestId
            order by c.displayOrder asc
            """)
    List<PlaceCandidate> findAllByAiRequestId(@Param("aiRequestId") Long aiRequestId);

    /** 이미 후보를 만들어 둔 요청인지 (같은 요청으로 후보가 두 번 만들어지지 않게). */
    boolean existsByAiRequest_Id(Long aiRequestId);

    @Query("""
            select c from PlaceCandidate c
            left join fetch c.place
            left join fetch c.aiRequest
            where c.id in :ids
            order by c.displayOrder asc
            """)
    List<PlaceCandidate> findAllByIdIn(@Param("ids") Collection<Long> ids);
}
