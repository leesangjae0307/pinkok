package com.example.pinkok_backend.seed;

import com.example.pinkok_backend.entity.TravelStyle;
import com.example.pinkok_backend.repository.TravelStyleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 서버가 켜질 때 여행 스타일 5종(FOOD / SIGHT / NATURE / SHOPPING / CAFE)을 DB에 채운다.
 * 이미 들어 있는 code 는 건너뛰므로 여러 번 켜도 중복되지 않는다. (AvatarSeeder 와 같은 방식)
 */
@Component
public class TravelStyleSeeder implements ApplicationRunner {

    private record StyleSeed(String code, String name) {
    }

    private static final List<StyleSeed> SEEDS = List.of(
            new StyleSeed("FOOD", "맛집 위주"),
            new StyleSeed("SIGHT", "관광지"),
            new StyleSeed("NATURE", "자연"),
            new StyleSeed("SHOPPING", "쇼핑"),
            new StyleSeed("CAFE", "카페")
    );

    private final TravelStyleRepository travelStyleRepository;

    public TravelStyleSeeder(TravelStyleRepository travelStyleRepository) {
        this.travelStyleRepository = travelStyleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    /** 없는 스타일만 추가한다. 추가한 개수를 돌려준다. */
    @Transactional
    public int seed() {
        Set<String> existing = travelStyleRepository.findAll().stream()
                .map(TravelStyle::getCode)
                .collect(Collectors.toSet());

        int added = 0;
        for (StyleSeed seed : SEEDS) {
            if (existing.contains(seed.code())) {
                continue;
            }
            TravelStyle style = new TravelStyle();
            style.setCode(seed.code());
            style.setName(seed.name());
            travelStyleRepository.save(style);
            added++;
        }
        return added;
    }
}
