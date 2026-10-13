package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import java.util.List;

@Getter
@Setter
public class TripCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 100)
    private String region;

    private LocalDate startDate;

    private LocalDate endDate;

    /** SOLO / COUPLE / FAMILY / FRIEND */
    private String companionType;

    private Boolean isPublic;

    /**
     * 함께 갈 팀원의 아이디(@username) 목록. 비워두면 혼자 시작하고 나중에 초대해도 된다.
     *
     * <p>여기 적힌 사람들에게는 초대장이 가고 알림이 뜬다. <b>수락해야</b> 팀원이 되므로,
     * 여행을 만든 직후에는 아직 나 혼자다.
     *
     * <p>닉네임이 아니라 아이디인 이유: 닉네임은 중복될 수 있어 누구인지 특정할 수 없다.
     * 앱에서는 GET /users/search 로 찾아 고르게 하고, 고른 사람의 username 을 보내면 된다.
     */
    private List<String> inviteeUsernames;
}
