package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원 탈퇴. 되돌릴 수 없는 동작이라 비밀번호를 한 번 더 확인한다
 * (폰을 잠깐 빌려준 사이에 눌리는 것 같은 사고를 막기 위해).
 */
@Getter
@Setter
public class AccountDeleteRequest {

    @NotBlank
    private String password;
}
