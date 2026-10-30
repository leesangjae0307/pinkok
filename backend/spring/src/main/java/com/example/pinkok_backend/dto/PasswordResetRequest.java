package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 인증번호 발송 요청. */
@Getter
@Setter
public class PasswordResetRequest {

    @NotBlank
    @Email
    private String email;
}
