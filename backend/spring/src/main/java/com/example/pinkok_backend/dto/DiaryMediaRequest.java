package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * 기록에 붙일 사진/영상 한 개.
 * 파일은 먼저 업로드 API(POST /files)로 올리고, 거기서 받은 url 을 여기에 넣는다.
 */
@Getter
@Setter
public class DiaryMediaRequest {

    /** 업로드 API가 돌려준 원본 주소 (예: /files/2026/09/abc.jpg) */
    @NotBlank
    private String fileUrl;

    /** 영상 길이(ms). 사진이면 비워둔다. */
    @Positive
    private Integer durationMs;
}
