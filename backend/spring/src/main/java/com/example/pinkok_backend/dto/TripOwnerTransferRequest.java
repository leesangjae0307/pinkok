package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 여행 주인(OWNER)을 다른 팀원에게 넘긴다. */
@Getter
@Setter
public class TripOwnerTransferRequest {

    /** 새 주인이 될 팀원의 userId (GET /trip-members 목록에서 고른다) */
    @NotNull
    private Long newOwnerUserId;
}
