package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.PlaceInput;
import com.example.pinkok_backend.entity.Place;
import com.example.pinkok_backend.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    /**
     * kakaoPlaceId 가 이미 있는 장소면 그걸 재사용하고, 없으면 새로 만든다.
     * 카카오맵 검색이 붙기 전까지는 프론트가 kakaoPlaceId 없이 직접 입력해서 써도 된다
     * (그 경우 매번 새 장소로 저장됨).
     */
    @Transactional
    public Place findOrCreate(PlaceInput input) {
        if (StringUtils.hasText(input.getKakaoPlaceId())) {
            return placeRepository.findByKakaoPlaceId(input.getKakaoPlaceId())
                    .orElseGet(() -> create(input));
        }
        return create(input);
    }

    private Place create(PlaceInput input) {
        LocalDateTime now = LocalDateTime.now();

        Place place = new Place();
        place.setKakaoPlaceId(input.getKakaoPlaceId());
        place.setName(input.getName());
        place.setRoadAddress(input.getRoadAddress());
        place.setLotAddress(input.getLotAddress());
        place.setLatitude(input.getLatitude());
        place.setLongitude(input.getLongitude());
        place.setCategoryGroupCode(input.getCategoryGroupCode());
        place.setCategoryName(input.getCategoryName());
        place.setPhone(input.getPhone());
        place.setPlaceUrl(input.getPlaceUrl());
        place.setCreatedAt(now);
        place.setUpdatedAt(now);

        return placeRepository.save(place);
    }
}
