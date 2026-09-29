package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiRequestCreateRequest {

    /** 나중에 여행을 골라도 되므로 선택 사항. */
    private Long tripId;

    /** LINK / TEXT / IMAGE */
    @NotBlank
    private String inputType;

    /** inputType=LINK 일 때 */
    private String sourceUrl;

    /** inputType=TEXT 일 때 */
    private String sourceText;

    /** inputType=IMAGE 일 때. POST /files 로 먼저 올린 뒤 받은 주소들. */
    private List<String> imageUrls;
}
