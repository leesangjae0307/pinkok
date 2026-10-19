package com.example.pinkok_backend.trip;

import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 여행 초대와 알림.
 *
 * <p>핵심 규칙: <b>수락해야 팀원이 된다.</b> 초대장(trip_invitations)과 참여자(trip_members)가
 * 다른 테이블이라, 아직 답하지 않았거나 거절한 사람은 남의 여행을 볼 수 없어야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripInvitationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    // ---------- 준비용 도우미 ----------

    private String signupAndLogin(String username, String nickname) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s@pinkok.com\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"%s\"}",
                                username, username, nickname)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s@pinkok.com\",\"password\":\"pass1234\"}", username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private Long createTrip(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String invitations(String token) throws Exception {
        return mockMvc.perform(get("/trip-invitations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String notifications(String token) throws Exception {
        return mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String trips(String token) throws Exception {
        return mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------- 테스트 ----------

    @Test
    @DisplayName("여행을 만들면서 초대하면 초대장과 알림이 함께 생긴다")
    void createTripWithInvitees() throws Exception {
        String owner = signupAndLogin("gamjieun", "감지은");
        signupAndLogin("jeonghun", "박정훈");

        Long tripId = createTrip(owner,
                "{\"title\":\"제주도 여행\",\"region\":\"제주\",\"inviteeUsernames\":[\"jeonghun\"]}");

        String mate = signupAndLoginAgain("jeonghun");

        // 받은 초대에 보인다
        String invitationJson = invitations(mate);
        assertThat((List<?>) JsonPath.read(invitationJson, "$")).hasSize(1);
        assertThat((String) JsonPath.read(invitationJson, "$[0].tripTitle")).isEqualTo("제주도 여행");
        assertThat((String) JsonPath.read(invitationJson, "$[0].status")).isEqualTo("PENDING");
        assertThat((String) JsonPath.read(invitationJson, "$[0].inviter.nickname")).isEqualTo("감지은");

        // 알림도 와 있다
        String notificationJson = notifications(mate);
        assertThat((String) JsonPath.read(notificationJson, "$[0].type")).isEqualTo("TRIP_INVITED");
        assertThat((String) JsonPath.read(notificationJson, "$[0].title")).contains("제주도 여행");
        assertThat((String) JsonPath.read(notificationJson, "$[0].targetType")).isEqualTo("INVITATION");
        assertThat((Boolean) JsonPath.read(notificationJson, "$[0].read")).isFalse();

        // 아직 수락 전이라 팀원이 아니다 — 여행 목록에도 없고 일정도 못 본다
        assertThat((List<?>) JsonPath.read(trips(mate), "$")).isEmpty();
        mockMvc.perform(get("/itinerary-items").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isForbidden());
    }

    /** 이미 가입한 계정으로 다시 로그인만 한다. */
    private String signupAndLoginAgain(String username) throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s@pinkok.com\",\"password\":\"pass1234\"}", username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    @DisplayName("수락하면 그때 팀원이 되고 내 여행 목록에 나타난다")
    void acceptInvitation() throws Exception {
        String owner = signupAndLogin("owner2", "주인장");
        String mate = signupAndLogin("mate2", "팀원");

        Long tripId = createTrip(owner, "{\"title\":\"부산 여행\",\"inviteeUsernames\":[\"mate2\"]}");
        Integer invitationId = JsonPath.read(invitations(mate), "$[0].id");

        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 여행 목록에 생기고, 일정도 볼 수 있다
        assertThat((List<?>) JsonPath.read(trips(mate), "$")).hasSize(1);
        assertThat((String) JsonPath.read(trips(mate), "$[0].myRole")).isEqualTo("MEMBER");
        mockMvc.perform(get("/itinerary-items").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk());

        // 답했으므로 받은 초대 목록(기본: PENDING만)에서는 빠진다
        assertThat((List<?>) JsonPath.read(invitations(mate), "$")).isEmpty();
    }

    @Test
    @DisplayName("거절하면 팀원이 되지 않는다")
    void rejectInvitation() throws Exception {
        String owner = signupAndLogin("owner3", "주인장");
        String mate = signupAndLogin("mate3", "팀원");

        Long tripId = createTrip(owner, "{\"title\":\"강릉 여행\",\"inviteeUsernames\":[\"mate3\"]}");
        Integer invitationId = JsonPath.read(invitations(mate), "$[0].id");

        mockMvc.perform(post("/trip-invitations/" + invitationId + "/reject")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat((List<?>) JsonPath.read(trips(mate), "$")).isEmpty();
        mockMvc.perform(get("/itinerary-items").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("한 번 답한 초대에는 다시 답할 수 없고(409), 남의 초대에는 답할 수 없다(403)")
    void respondTwiceOrToOthersInvitation() throws Exception {
        String owner = signupAndLogin("owner4", "주인장");
        String mate = signupAndLogin("mate4", "팀원");
        String stranger = signupAndLogin("stranger4", "남남이");

        createTrip(owner, "{\"title\":\"여수 여행\",\"inviteeUsernames\":[\"mate4\"]}");
        Integer invitationId = JsonPath.read(invitations(mate), "$[0].id");

        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk());

        mockMvc.perform(post("/trip-invitations/" + invitationId + "/reject")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("같은 사람을 두 번 초대하거나 이미 팀원인 사람을 초대하면 409")
    void duplicateInvitation() throws Exception {
        String owner = signupAndLogin("owner5", "주인장");
        String mate = signupAndLogin("mate5", "팀원");

        Long tripId = createTrip(owner, "{\"title\":\"속초 여행\",\"inviteeUsernames\":[\"mate5\"]}");

        // 답을 기다리는 중에 또 보내기
        mockMvc.perform(post("/trip-invitations")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"username\":\"mate5\"}", tripId)))
                .andExpect(status().isConflict());

        // 수락해서 팀원이 된 뒤에 또 보내기
        Integer invitationId = JsonPath.read(invitations(mate), "$[0].id");
        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk());

        mockMvc.perform(post("/trip-invitations")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"username\":\"mate5\"}", tripId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("없는 아이디로 초대하면 404, 자기 자신은 400")
    void invalidInvitee() throws Exception {
        String owner = signupAndLogin("owner6", "주인장");

        mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"없는사람 초대\",\"inviteeUsernames\":[\"nobody-here\"]}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"나 자신 초대\",\"inviteeUsernames\":[\"owner6\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("팀원을 더 초대하는 건 여행을 만든 사람만 할 수 있다 (403)")
    void onlyOwnerCanInvite() throws Exception {
        String owner = signupAndLogin("owner7", "주인장");
        String mate = signupAndLogin("mate7", "팀원");
        signupAndLogin("third7", "세번째");

        Long tripId = createTrip(owner, "{\"title\":\"통영 여행\",\"inviteeUsernames\":[\"mate7\"]}");
        Integer invitationId = JsonPath.read(invitations(mate), "$[0].id");
        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk());

        // 팀원이 됐어도 초대 권한은 없다
        mockMvc.perform(post("/trip-invitations")
                        .header("Authorization", "Bearer " + mate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"username\":\"third7\"}", tripId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("사용자 검색 - 아이디와 닉네임 둘 다로 찾고, 나는 빠지고, 이미 팀원인 사람은 표시된다")
    void searchUsers() throws Exception {
        String owner = signupAndLogin("jieun01", "감지은");
        String mate = signupAndLogin("jieun02", "김지은");
        signupAndLogin("other01", "지은짱");

        Long tripId = createTrip(owner, "{\"title\":\"검색 테스트\",\"inviteeUsernames\":[\"jieun02\"]}");

        // 닉네임으로 검색 — 내 것(감지은)은 빠지고 나머지 둘이 나온다
        mockMvc.perform(get("/users/search").param("keyword", "지은")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 아이디로 검색 + tripId 를 주면 초대 중인지 표시된다
        mockMvc.perform(get("/users/search")
                        .param("keyword", "jieun02")
                        .param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("jieun02"))
                .andExpect(jsonPath("$[0].nickname").value("김지은"))
                .andExpect(jsonPath("$[0].alreadyInvited").value(true))
                .andExpect(jsonPath("$[0].alreadyMember").value(false));

        // 수락해서 팀원이 되면 alreadyMember 로 바뀐다
        Integer invitationId = JsonPath.read(invitations(mate), "$[0].id");
        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/search")
                        .param("keyword", "jieun02")
                        .param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$[0].alreadyMember").value(true));

        // 한 글자는 400 (검색 결과가 너무 많아진다)
        mockMvc.perform(get("/users/search").param("keyword", "지")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("알림 - 읽음 처리와 안 읽은 개수")
    void notificationRead() throws Exception {
        String owner = signupAndLogin("owner9", "주인장");
        String mate = signupAndLogin("mate9", "팀원");

        createTrip(owner, "{\"title\":\"알림 테스트1\",\"inviteeUsernames\":[\"mate9\"]}");
        createTrip(owner, "{\"title\":\"알림 테스트2\",\"inviteeUsernames\":[\"mate9\"]}");

        mockMvc.perform(get("/notifications/unread-count").header("Authorization", "Bearer " + mate))
                .andExpect(jsonPath("$.count").value(2));

        Integer notificationId = JsonPath.read(notifications(mate), "$[0].id");
        mockMvc.perform(patch("/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/notifications/unread-count").header("Authorization", "Bearer " + mate))
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(patch("/notifications/read-all").header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));

        mockMvc.perform(get("/notifications/unread-count").header("Authorization", "Bearer " + mate))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("남의 알림은 읽음 처리할 수 없다 (403), 토큰 없으면 401")
    void notificationPermission() throws Exception {
        String owner = signupAndLogin("owner10", "주인장");
        String mate = signupAndLogin("mate10", "팀원");

        createTrip(owner, "{\"title\":\"권한 테스트\",\"inviteeUsernames\":[\"mate10\"]}");
        Integer notificationId = JsonPath.read(notifications(mate), "$[0].id");

        mockMvc.perform(patch("/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/trip-invitations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/search").param("keyword", "지은"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("여행의 초대 현황은 팀원이면 볼 수 있다")
    void invitationsOfTrip() throws Exception {
        String owner = signupAndLogin("owner11", "주인장");
        signupAndLogin("mate11", "팀원");
        String stranger = signupAndLogin("stranger11", "남남이");

        Long tripId = createTrip(owner, "{\"title\":\"현황 테스트\",\"inviteeUsernames\":[\"mate11\"]}");

        mockMvc.perform(get("/trip-invitations/trip").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].invitee.username").value("mate11"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(get("/trip-invitations/trip").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
    }
}
