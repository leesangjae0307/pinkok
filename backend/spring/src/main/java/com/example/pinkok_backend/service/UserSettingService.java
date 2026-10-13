package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.UserSettingResponse;
import com.example.pinkok_backend.dto.UserSettingUpdateRequest;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.entity.UserSetting;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.repository.UserSettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * 사용자 설정 (알림 받기 · 테마 · 언어).
 *
 * <p>설정 행은 가입할 때 만들지 않고, <b>처음 설정 화면을 열 때 기본값으로 만든다.</b>
 * 이미 가입한 사람들에게도 따로 손대지 않고 똑같이 동작하게 하기 위해서다.
 *
 * <p>테마·언어는 컬럼만 두고 바꾸는 기능은 넣지 않았다. 앱 디자인이 도트 테마 하나뿐이고
 * 한국어만 지원해서, 지금 고르게 해봐야 고를 게 없다. 늘어나면 그때 열면 된다.
 */
@Service
public class UserSettingService {

    private static final String DEFAULT_THEME = "PIXEL_MINT";
    private static final String DEFAULT_LANGUAGE = "ko";

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;

    public UserSettingService(UserSettingRepository userSettingRepository, UserRepository userRepository) {
        this.userSettingRepository = userSettingRepository;
        this.userRepository = userRepository;
    }

    /** 설정 조회. 아직 없으면 기본값으로 만들어서 돌려준다. */
    @Transactional
    public UserSettingResponse get(Long userId) {
        return UserSettingResponse.from(getOrCreate(userId));
    }

    /** 보낸 항목만 바꾼다. */
    @Transactional
    public UserSettingResponse update(Long userId, UserSettingUpdateRequest request) {
        UserSetting setting = getOrCreate(userId);

        if (request.getNotificationEnabled() != null) {
            setting.setNotificationEnabled(request.getNotificationEnabled());
        }
        setting.setUpdatedAt(LocalDateTime.now());
        return UserSettingResponse.from(userSettingRepository.save(setting));
    }

    /**
     * 알림을 받기로 한 사람인지. 설정을 한 번도 열지 않았으면 기본값(받음)으로 본다.
     *
     * <p>지금은 앱 안 알림함에는 끄더라도 계속 쌓인다. 끈 사람의 알림을 아예 만들지 않으면
     * 여행 초대를 받고도 영영 모를 수 있기 때문이다(초대는 알림함 말고 초대함에도 있지만,
     * 기록을 지워버리는 쪽이 더 위험하다). 이 값은 나중에 폰 푸시(FCM)를 붙일 때
     * "푸시를 보낼지"를 정하는 데 쓴다.
     */
    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Long userId) {
        return userSettingRepository.findByUserId(userId)
                .map(setting -> Boolean.TRUE.equals(setting.getNotificationEnabled()))
                .orElse(true);
    }

    private UserSetting getOrCreate(Long userId) {
        return userSettingRepository.findByUserId(userId).orElseGet(() -> create(userId));
    }

    private UserSetting create(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        UserSetting setting = new UserSetting();
        setting.setUser(user);
        setting.setNotificationEnabled(true);
        setting.setTheme(DEFAULT_THEME);
        setting.setLanguage(DEFAULT_LANGUAGE);
        setting.setUpdatedAt(LocalDateTime.now());
        return userSettingRepository.save(setting);
    }

}
