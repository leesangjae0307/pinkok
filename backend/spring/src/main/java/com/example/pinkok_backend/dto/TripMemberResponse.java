package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.entity.User;

import java.time.LocalDateTime;

/**
 * 여행 참여자 한 명. 다른 팀원에게 보여주는 정보라 이메일·아이디 같은 개인정보는 넣지 않는다.
 *
 * @param me 요청한 사람 본인인지 (앱에서 "나" 표시나 "나가기" 버튼에 쓴다)
 */
public record TripMemberResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String avatarImageUrl,
        String role,
        LocalDateTime joinedAt,
        boolean me
) {
    public static TripMemberResponse of(TripMember member, Long requesterId) {
        User user = member.getUser();
        return new TripMemberResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getAvatar() == null ? null : user.getAvatar().getImageUrl(),
                member.getRole(),
                member.getJoinedAt(),
                user.getId().equals(requesterId));
    }
}
