package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripInvitation;
import com.example.pinkok_backend.entity.User;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 초대장 한 건.
 *
 * <p>받는 쪽 화면("'감지은'님이 '제주도 여행'에 초대했어요 [수락] [거절]")과
 * 보낸 쪽 화면(여행별 초대 현황)에서 같이 쓴다.
 */
@Getter
public class TripInvitationResponse {

    private final Long id;
    private final Long tripId;
    private final String tripTitle;
    private final String tripRegion;
    private final LocalDate startDate;
    private final LocalDate endDate;
    /** PENDING / ACCEPTED / REJECTED */
    private final String status;
    private final Person inviter;
    private final Person invitee;
    private final LocalDateTime createdAt;
    private final LocalDateTime respondedAt;

    private TripInvitationResponse(TripInvitation invitation) {
        Trip trip = invitation.getTrip();

        this.id = invitation.getId();
        this.tripId = trip.getId();
        this.tripTitle = trip.getTitle();
        this.tripRegion = trip.getRegion();
        this.startDate = trip.getStartDate();
        this.endDate = trip.getEndDate();
        this.status = invitation.getStatus();
        this.inviter = Person.of(invitation.getInviter());
        this.invitee = Person.of(invitation.getInvitee());
        this.createdAt = invitation.getCreatedAt();
        this.respondedAt = invitation.getRespondedAt();
    }

    public static TripInvitationResponse from(TripInvitation invitation) {
        return new TripInvitationResponse(invitation);
    }

    /** 초대를 주고받는 사람. 목록에 얼굴과 이름을 띄우는 데 필요한 것만 담는다. */
    @Getter
    public static class Person {

        private final Long userId;
        private final String username;
        private final String nickname;
        private final String profileImageUrl;
        private final String avatarImageUrl;

        private Person(User user) {
            this.userId = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.profileImageUrl = user.getProfileImageUrl();
            this.avatarImageUrl = user.getAvatar() == null ? null : user.getAvatar().getImageUrl();
        }

        static Person of(User user) {
            return user == null ? null : new Person(user);
        }
    }
}
