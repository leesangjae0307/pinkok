package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NicknameUpdateRequest {

    /** 화면에 표시되는 이름. 회원가입과 같은 규칙. */
    @NotBlank
    @Size(max = 50)
    private String nickname;
}
