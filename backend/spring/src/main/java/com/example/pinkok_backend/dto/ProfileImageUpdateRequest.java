package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileImageUpdateRequest {

    /** POST /files 로 사진을 올리고 돌려받은 url (예: /files/2026/09/abc.jpg) */
    @NotBlank
    private String profileImageUrl;
}
