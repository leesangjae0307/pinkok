package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

}
