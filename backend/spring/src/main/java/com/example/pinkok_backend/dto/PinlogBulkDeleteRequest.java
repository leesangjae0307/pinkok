package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PinLog 여러 개 한 번에 삭제.
 *
 * @param ids 지울 PinLog id 들 (1~50개). 하나라도 없거나 내가 만든 것이 아니면 아무것도 지우지 않는다
 */
public record PinlogBulkDeleteRequest(
        @NotEmpty(message = "지울 PinLog 를 하나 이상 골라 주세요.")
        @Size(max = 50, message = "한 번에 50개까지 지울 수 있습니다.")
        List<@NotNull Long> ids
) {
}
