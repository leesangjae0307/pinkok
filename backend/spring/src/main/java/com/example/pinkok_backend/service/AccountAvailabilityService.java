package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.SignupRequest;
import com.example.pinkok_backend.repository.UserRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 가입 화면의 중복 확인. 형식 규칙은 회원가입 요청({@link SignupRequest})의 검증 어노테이션을 그대로 가져다 써서,
 * "확인은 통과했는데 가입에서 형식 오류" 같은 어긋남이 생기지 않게 한다.
 *
 * <p>확인과 가입 사이에 다른 사람이 먼저 가입할 수 있으므로, 이 결과는 안내일 뿐이고 최종 판단은 가입 API 가 한다.
 */
@Service
public class AccountAvailabilityService {

    private static final String BAD_FORMAT = "형식이 올바르지 않습니다.";
    /** users.email 컬럼(255자)에 들어갈 수 있는 길이. */
    private static final int MAX_EMAIL_LENGTH = 254;

    private final UserRepository userRepository;
    private final Validator validator;

    public AccountAvailabilityService(UserRepository userRepository, Validator validator) {
        this.userRepository = userRepository;
        this.validator = validator;
    }

    public boolean isEmailAvailable(String email) {
        requireValid("email", email);
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_FORMAT);
        }
        return !userRepository.existsByEmail(email);
    }

    public boolean isUsernameAvailable(String username) {
        requireValid("username", username);
        return !userRepository.existsByUsername(username);
    }

    private void requireValid(String property, String value) {
        if (value == null || !validator.validateValue(SignupRequest.class, property, value).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_FORMAT);
        }
    }
}
