package com.example.pinkok_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 여행 자유 해시태그 정리 규칙.
 *
 * <ul>
 *   <li>앞의 # 은 몇 개든 떼고 저장한다 ("#부산" → "부산"). 응답에도 # 없이 주고, 앱이 # 을 붙여 보여준다.</li>
 *   <li>공백은 전부 지운다 ("부산 여행" → "부산여행") — 해시태그는 띄어쓰기가 없다.</li>
 *   <li>정리한 뒤 비면 버린다 ("#", " ").</li>
 *   <li>대소문자를 무시하고 같은 태그는 하나만 남긴다 (처음 쓴 표기 유지).</li>
 *   <li>최대 {@value #MAX_TAGS}개, 각 1~{@value #MAX_LENGTH}자. 넘으면 400.</li>
 *   <li>보낸 순서를 지킨다.</li>
 * </ul>
 */
final class TripTags {

    static final int MAX_TAGS = 10;
    static final int MAX_LENGTH = 20;

    private TripTags() {
    }

    static List<String> normalize(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            String tag = value.replaceAll("\\s+", "").replaceFirst("^#+", "");
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.contains("#")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "태그 안에는 # 을 쓸 수 없습니다: " + value);
            }
            if (tag.length() > MAX_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "태그는 " + MAX_LENGTH + "자까지 쓸 수 있습니다: " + value);
            }
            if (seen.add(tag.toLowerCase(Locale.ROOT))) {
                tags.add(tag);
            }
        }
        if (tags.size() > MAX_TAGS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "태그는 " + MAX_TAGS + "개까지 달 수 있습니다.");
        }
        return tags;
    }
}
