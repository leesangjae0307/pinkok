package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

}
