package com.example.pinkok_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 기록 수정. 보낸 항목만 바뀐다(안 보낸 항목은 그대로).
 * media 를 보내면 기존 첨부는 전부 지워지고 보낸 목록으로 통째로 바뀐다.
 */
@Getter
@Setter
public class DiaryUpdateRequest {

    @Size(max = 2000)
    private String content;

    @Min(1)
    @Max(5)
    private Integer rating;

    @Valid
    private List<DiaryMediaRequest> media;
}
