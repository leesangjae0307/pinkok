package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {

    private final Long id;
    private final String email;
    private final String username;
    private final String nickname;
    private final String profileImageUrl;
    private final AvatarResponse avatar;

    private UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.avatar = user.getAvatar() == null ? null : AvatarResponse.from(user.getAvatar());
    }

    public static UserResponse from(User user) {
        return new UserResponse(user);
    }
}
