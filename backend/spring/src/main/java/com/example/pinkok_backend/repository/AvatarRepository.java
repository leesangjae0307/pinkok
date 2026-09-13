package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {

    List<Avatar> findAllByOrderByDisplayOrderAsc();

}
