package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.UserTravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTravelStyleRepository extends JpaRepository<UserTravelStyle, UserTravelStyle.UserTravelStyleId> {

    List<UserTravelStyle> findAllByUser_Id(Long userId);

    @Query("select s.name from UserTravelStyle u join u.travelStyle s where u.user.id = :userId order by s.id")
    List<String> findStyleNamesByUserId(@Param("userId") Long userId);

}
