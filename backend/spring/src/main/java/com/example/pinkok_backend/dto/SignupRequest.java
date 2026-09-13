package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    /** @아이디 (로그인/멘션용 핸들). 영문 소문자·숫자·언더스코어. */
    @NotBlank
    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[a-z0-9_]+$", message = "영문 소문자, 숫자, 밑줄(_)만 사용할 수 있습니다.")
    private String username;

    /** 화면에 표시되는 이름. */
    @NotBlank
    @Size(max = 50)
    private String nickname;

    /** 기본 도트 아바타 선택(선택 사항). 나중에 프로필에서 바꿀 수 있어 회원가입 때는 필수로 두지 않는다. */
    private Long avatarId;
}
