package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AiRequestRepository extends JpaRepository<AiRequest, Long> {

    List<AiRequest> findAllByUser_IdOrderByRequestedAtDesc(Long userId);

    List<AiRequest> findAllByUser_IdAndTrip_IdOrderByRequestedAtDesc(Long userId, Long tripId);

    /** 서버가 꺼지면서 끝내지 못한 요청을 찾는다. */
    List<AiRequest> findAllByStatusInAndRequestedAtBefore(Collection<String> statuses, LocalDateTime before);

}
