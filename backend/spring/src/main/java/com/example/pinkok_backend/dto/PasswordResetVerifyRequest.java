package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** 이메일로 받은 인증번호 확인. */
@Getter
@Setter
public class PasswordResetVerifyRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 숫자 6자리입니다.")
    private String code;
}
