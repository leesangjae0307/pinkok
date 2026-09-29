package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.User;
import lombok.Getter;

/**
 * 기록을 쓴 사람 정보. 목록에서 "누구의 기록인지" 보여줄 때만 쓰므로
 * 이메일·아이디 같은 개인정보는 넣지 않는다.
 */
@Getter
public class DiaryAuthorResponse {

    private final Long userId;
    private final String nickname;
    private final String profileImageUrl;
    private final String avatarImageUrl;

    private DiaryAuthorResponse(User user) {
        this.userId = user.getId();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.avatarImageUrl = user.getAvatar() == null ? null : user.getAvatar().getImageUrl();
    }

    public static DiaryAuthorResponse from(User user) {
        return new DiaryAuthorResponse(user);
    }
}
