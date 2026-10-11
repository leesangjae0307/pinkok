package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TravelStyleResponse;
import com.example.pinkok_backend.entity.TravelStyle;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.entity.UserTravelStyle;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.repository.UserTravelStyleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 사용자 본인의 기본 여행 취향. AI 추천에서 여행·요청에 취향이 없을 때 기본값으로 쓰인다. */
@Service
public class UserTravelStyleService {

    private final UserTravelStyleRepository userTravelStyleRepository;
    private final UserRepository userRepository;
    private final TravelStyleService travelStyleService;

    public UserTravelStyleService(UserTravelStyleRepository userTravelStyleRepository,
                                  UserRepository userRepository,
                                  TravelStyleService travelStyleService) {
        this.userTravelStyleRepository = userTravelStyleRepository;
        this.userRepository = userRepository;
        this.travelStyleService = travelStyleService;
    }

    @Transactional(readOnly = true)
    public List<TravelStyleResponse> list(Long userId) {
        return userTravelStyleRepository.findAllByUser_Id(userId).stream()
                .map(link -> TravelStyleResponse.from(link.getTravelStyle()))
                .sorted(java.util.Comparator.comparing(TravelStyleResponse::id))
                .toList();
    }

    /** 통째로 교체. 빠진 것은 해제, 새로 생긴 것은 추가하고 그대로인 것은 건드리지 않는다. */
    @Transactional
    public List<TravelStyleResponse> replace(Long userId, List<String> styleCodes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        List<TravelStyle> wanted = travelStyleService.resolve(styleCodes);
        Set<Long> wantedIds = wanted.stream().map(TravelStyle::getId).collect(Collectors.toSet());

        List<UserTravelStyle> current = userTravelStyleRepository.findAllByUser_Id(userId);
        Set<Long> currentIds = current.stream().map(link -> link.getTravelStyle().getId()).collect(Collectors.toSet());

        userTravelStyleRepository.deleteAll(current.stream()
                .filter(link -> !wantedIds.contains(link.getTravelStyle().getId()))
                .toList());

        for (TravelStyle style : wanted) {
            if (!currentIds.contains(style.getId())) {
                UserTravelStyle link = new UserTravelStyle();
                UserTravelStyle.UserTravelStyleId id = new UserTravelStyle.UserTravelStyleId();
                id.setUserId(userId);
                id.setStyleId(style.getId());
                link.setId(id);
                link.setUser(user);
                link.setTravelStyle(style);
                userTravelStyleRepository.save(link);
            }
        }
        return wanted.stream().map(TravelStyleResponse::from)
                .sorted(java.util.Comparator.comparing(TravelStyleResponse::id)).toList();
    }
}
