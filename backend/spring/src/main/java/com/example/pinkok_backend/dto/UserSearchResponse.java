package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.User;
import lombok.Getter;

/**
 * 팀원 검색 결과 한 명.
 *
 * <p>닉네임은 중복될 수 있으므로 화면에 <b>아이디(@username)를 같이</b> 보여줘야 누구인지 구분된다.
 * 초대할 때 서버로 보내는 값도 아이디다.
 */
@Getter
public class UserSearchResponse {

    private final String username;
    private final String nickname;
    private final String profileImageUrl;
    private final String avatarImageUrl;
    /** 이미 이 여행의 팀원인지 (tripId 를 같이 보냈을 때만 의미 있음) */
    private final boolean alreadyMember;
    /** 이미 초대를 보내 답을 기다리는 중인지 */
    private final boolean alreadyInvited;

    private UserSearchResponse(User user, boolean alreadyMember, boolean alreadyInvited) {
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.avatarImageUrl = user.getAvatar() == null ? null : user.getAvatar().getImageUrl();
        this.alreadyMember = alreadyMember;
        this.alreadyInvited = alreadyInvited;
    }

    public static UserSearchResponse of(User user, boolean alreadyMember, boolean alreadyInvited) {
        return new UserSearchResponse(user, alreadyMember, alreadyInvited);
    }
}
