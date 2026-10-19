package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 여행을 만든 뒤에 팀원을 더 초대할 때. */
@Getter
@Setter
public class TripInvitationCreateRequest {

    @NotNull
    private Long tripId;

    /** 초대할 사람의 아이디(@username). 닉네임이 아니라 아이디다. */
    @NotBlank
    private String username;
}
