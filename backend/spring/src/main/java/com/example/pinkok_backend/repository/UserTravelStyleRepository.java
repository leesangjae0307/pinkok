package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.UserTravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTravelStyleRepository extends JpaRepository<UserTravelStyle, UserTravelStyle.UserTravelStyleId> {

}
