package com.example.pinkok_backend.seed;

import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.repository.AvatarRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 서버가 켜질 때 기본 도트 아바타 6종(남3 · 여3)을 DB에 채운다.
 * 이미 들어 있는 code 는 건너뛰므로 여러 번 켜도 중복되지 않는다.
 * 이미지 파일은 resources/static/images/avatars/ 에 있고, /images/avatars/xx.png 주소로 열린다.
 */
@Component
public class AvatarSeeder implements ApplicationRunner {

    private record AvatarSeed(String code, String name, String gender, int displayOrder) {
    }

    private static final List<AvatarSeed> SEEDS = List.of(
            new AvatarSeed("M1", "남자1", "MALE", 1),
            new AvatarSeed("M2", "남자2", "MALE", 2),
            new AvatarSeed("M3", "남자3", "MALE", 3),
            new AvatarSeed("F1", "여자1", "FEMALE", 4),
            new AvatarSeed("F2", "여자2", "FEMALE", 5),
            new AvatarSeed("F3", "여자3", "FEMALE", 6)
    );

    private final AvatarRepository avatarRepository;

    public AvatarSeeder(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    /** 없는 아바타만 추가한다. 추가한 개수를 돌려준다. */
    @Transactional
    public int seed() {
        Set<String> existingCodes = avatarRepository.findAll().stream()
                .map(Avatar::getCode)
                .collect(Collectors.toSet());

        int added = 0;
        for (AvatarSeed seed : SEEDS) {
            if (existingCodes.contains(seed.code())) {
                continue;
            }
            Avatar avatar = new Avatar();
            avatar.setCode(seed.code());
            avatar.setName(seed.name());
            avatar.setGender(seed.gender());
            avatar.setDisplayOrder(seed.displayOrder());
            avatar.setImageUrl("/images/avatars/" + seed.code().toLowerCase() + ".png");
            avatarRepository.save(avatar);
            added++;
        }
        return added;
    }
}
