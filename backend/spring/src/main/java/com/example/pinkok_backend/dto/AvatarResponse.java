package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Avatar;
import lombok.Getter;

@Getter
public class AvatarResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final String imageUrl;
    private final String gender;

    private AvatarResponse(Avatar avatar) {
        this.id = avatar.getId();
        this.code = avatar.getCode();
        this.name = avatar.getName();
        this.imageUrl = avatar.getImageUrl();
        this.gender = avatar.getGender();
    }

    public static AvatarResponse from(Avatar avatar) {
        return new AvatarResponse(avatar);
    }
}
