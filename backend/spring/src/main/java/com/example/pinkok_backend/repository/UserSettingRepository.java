package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

}
