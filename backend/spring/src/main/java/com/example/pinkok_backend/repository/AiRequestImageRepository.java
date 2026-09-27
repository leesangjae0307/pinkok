package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.AiRequestImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRequestImageRepository extends JpaRepository<AiRequestImage, Long> {

    List<AiRequestImage> findAllByAiRequest_IdOrderByDisplayOrderAsc(Long aiRequestId);

}
