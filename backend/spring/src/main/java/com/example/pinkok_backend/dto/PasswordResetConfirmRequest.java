package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetConfirmRequest {

    @NotBlank
    private String resetToken;

    /** 회원가입과 같은 규칙. */
    @NotBlank
    @Size(min = 8, max = 64)
    private String newPassword;
}
