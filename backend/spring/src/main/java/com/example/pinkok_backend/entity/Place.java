package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "places")
@Getter
@Setter
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_place_id", unique = true, length = 50)
    private String kakaoPlaceId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "road_address", length = 300)
    private String roadAddress;

    @Column(name = "lot_address", length = 300)
    private String lotAddress;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "category_group_code", length = 20)
    private String categoryGroupCode;

    @Column(name = "category_name", length = 200)
    private String categoryName;

    @Column(length = 30)
    private String phone;

    @Column(name = "place_url", length = 500)
    private String placeUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
