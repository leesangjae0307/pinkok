package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TravelStyleResponse;
import com.example.pinkok_backend.entity.TravelStyle;
import com.example.pinkok_backend.repository.TravelStyleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class TravelStyleService {

    private final TravelStyleRepository travelStyleRepository;

    public TravelStyleService(TravelStyleRepository travelStyleRepository) {
        this.travelStyleRepository = travelStyleRepository;
    }

    @Transactional(readOnly = true)
    public List<TravelStyleResponse> list() {
        return travelStyleRepository.findAll().stream().map(TravelStyleResponse::from).toList();
    }

    /**
     * 요청으로 받은 style code 들을 스타일 엔티티로 바꾼다. 공백·대소문자·중복은 정리하고,
     * 모르는 code 가 하나라도 있으면 400 (오타를 조용히 무시하면 사용자는 저장된 줄 안다).
     */
    @Transactional(readOnly = true)
    public List<TravelStyle> resolve(List<String> styleCodes) {
        List<String> codes = styleCodes.stream()
                .filter(StringUtils::hasText)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        List<TravelStyle> found = travelStyleRepository.findAllByCodeIn(codes);
        if (found.size() != codes.size()) {
            List<String> known = found.stream().map(TravelStyle::getCode).toList();
            List<String> unknown = codes.stream().filter(c -> !known.contains(c)).toList();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "알 수 없는 styleCodes: " + String.join(", ", unknown) + " (GET /travel-styles 의 code 를 쓰세요)");
        }
        return found;
    }
}
