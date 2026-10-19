package com.example.pinkok_backend.trip;

import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 참여자 목록 · 내보내기(OWNER) · 나가기(MEMBER). 참여자 추가는 여행 초대의 몫이라 테스트에서는 직접 넣는다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripMemberTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TripMemberRepository tripMemberRepository;
    @Autowired
    TripRepository tripRepository;
    @Autowired
    UserRepository userRepository;

    private String signupAndLogin(String email, String username, String nickname) throws Exception {
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"%s\"}",
                                email, username, nickname)))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"pass1234\"}", email)))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private long userIdOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private long createTrip(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제주\"}"))
                .andExpect(status().isCreated()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** 초대 기능이 없으므로 참여자로 직접 넣는다 */
    private void addMember(long tripId, long userId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        TripMember member = new TripMember();
        TripMember.TripMemberId id = new TripMember.TripMemberId();
        id.setTripId(tripId);
        id.setUserId(userId);
        member.setId(id);
        member.setTrip(trip);
        member.setUser(user);
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now().plusSeconds(1));
        tripMemberRepository.saveAndFlush(member);
    }

    @Test
    @DisplayName("목록 - OWNER 가 맨 위, 닉네임·역할이 보이고 이메일은 안 보이며, 본인은 me=true")
    void list_ownerFirst_andHidesEmail() throws Exception {
        String owner = signupAndLogin("mem1@pinkok.com", "memuser1", "주인");
        String friend = signupAndLogin("mem1b@pinkok.com", "memuser1b", "친구");
        long tripId = createTrip(owner);
        addMember(tripId, userIdOf("mem1b@pinkok.com"));

        mockMvc.perform(get("/trip-members").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + friend))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].nickname").value("주인"))
                .andExpect(jsonPath("$[0].me").value(false))
                .andExpect(jsonPath("$[1].role").value("MEMBER"))
                .andExpect(jsonPath("$[1].me").value(true))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].username").doesNotExist());
    }

    @Test
    @DisplayName("목록 - 팀원이 아니면 403, 토큰이 없으면 401")
    void list_requiresMembership() throws Exception {
        String owner = signupAndLogin("mem2@pinkok.com", "memuser2", "주인");
        String stranger = signupAndLogin("mem2b@pinkok.com", "memuser2b", "남");
        long tripId = createTrip(owner);

        mockMvc.perform(get("/trip-members").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/trip-members").param("tripId", String.valueOf(tripId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내보내기 - OWNER 가 참여자를 내보내면 그 사람은 더 이상 이 여행에 접근할 수 없다")
    void remove_byOwner() throws Exception {
        String owner = signupAndLogin("mem3@pinkok.com", "memuser3", "주인");
        String friend = signupAndLogin("mem3b@pinkok.com", "memuser3b", "친구");
        long tripId = createTrip(owner);
        long friendId = userIdOf("mem3b@pinkok.com");
        addMember(tripId, friendId);

        mockMvc.perform(delete("/trip-members/" + friendId).param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/trip-members").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + friend))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내보내기 - 일반 참여자는 못 하고(403), OWNER 는 내보낼 수 없고(400), 없는 사람은 404")
    void remove_restrictions() throws Exception {
        String owner = signupAndLogin("mem4@pinkok.com", "memuser4", "주인");
        String friend = signupAndLogin("mem4b@pinkok.com", "memuser4b", "친구");
        String other = signupAndLogin("mem4c@pinkok.com", "memuser4c", "다른친구");
        long tripId = createTrip(owner);
        long ownerId = userIdOf("mem4@pinkok.com");
        long friendId = userIdOf("mem4b@pinkok.com");
        long otherId = userIdOf("mem4c@pinkok.com");
        addMember(tripId, friendId);
        addMember(tripId, otherId);

        // 일반 참여자가 다른 참여자를 내보내려 함
        mockMvc.perform(delete("/trip-members/" + otherId).param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + friend))
                .andExpect(status().isForbidden());
        // OWNER 가 자기 자신을 내보내려 함
        mockMvc.perform(delete("/trip-members/" + ownerId).param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
        // 참여자가 아닌 사람
        mockMvc.perform(delete("/trip-members/999999").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/trip-members").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("나가기 - 참여자는 나갈 수 있고, OWNER 는 400, 팀원이 아니면 403")
    void leave() throws Exception {
        String owner = signupAndLogin("mem5@pinkok.com", "memuser5", "주인");
        String friend = signupAndLogin("mem5b@pinkok.com", "memuser5b", "친구");
        String stranger = signupAndLogin("mem5c@pinkok.com", "memuser5c", "남");
        long tripId = createTrip(owner);
        addMember(tripId, userIdOf("mem5b@pinkok.com"));

        mockMvc.perform(delete("/trip-members/me").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/trip-members/me").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/trip-members/me").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + friend))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + friend))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/trip-members").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("내보내거나 나가도 그 사람이 꽂은 핀은 여행에 남는다")
    void pinsRemainAfterLeaving() throws Exception {
        String owner = signupAndLogin("mem6@pinkok.com", "memuser6", "주인");
        String friend = signupAndLogin("mem6b@pinkok.com", "memuser6b", "친구");
        long tripId = createTrip(owner);
        addMember(tripId, userIdOf("mem6b@pinkok.com"));

        mockMvc.perform(post("/itinerary-items").header("Authorization", "Bearer " + friend)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"tripId\":%d,\"place\":{\"name\":\"친구가 찍은 곳\",\"latitude\":33.4,\"longitude\":126.5}}", tripId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/trip-members/me").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + friend))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].place.name").value("친구가 찍은 곳"));
    }
}
