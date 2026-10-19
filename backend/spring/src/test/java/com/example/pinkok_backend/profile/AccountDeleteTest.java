package com.example.pinkok_backend.profile;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원 탈퇴 (소프트 삭제 + 개인정보 제거).
 *
 * <p>여행을 여럿이 함께 쓰기 때문에 남긴 기록까지 지우면 팀원 화면이 깨진다.
 * 그래서 계정은 지우되 기록은 남기고, 개인을 알아볼 수 있는 정보만 없앤다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountDeleteTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    com.example.pinkok_backend.repository.TripRepository tripRepository;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    private String signup(String username, String nickname) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s@pinkok.com\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"%s\"}",
                                username, username, nickname)))
                .andExpect(status().isCreated());
        return login(username);
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s@pinkok.com\",\"password\":\"pass1234\"}", username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String refreshTokenOf(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s@pinkok.com\",\"password\":\"pass1234\"}", username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private MvcResult withdraw(String token, String password) throws Exception {
        return mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"password\":\"%s\"}", password)))
                .andReturn();
    }

    @Test
    @DisplayName("탈퇴하면 다시 로그인할 수 없고, 가지고 있던 리프레시 토큰도 끊긴다")
    void withdrawBlocksLogin() throws Exception {
        signup("bye1", "안녕히");
        String refreshToken = refreshTokenOf("bye1");
        String accessToken = login("bye1");

        assertThat(withdraw(accessToken, "pass1234").getResponse().getStatus()).isEqualTo(204);

        // 다시 로그인 불가
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bye1@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isUnauthorized());

        // 남아 있던 리프레시 토큰으로도 재발급 불가
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"refreshToken\":\"%s\"}", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 탈퇴되지 않는다 (401)")
    void wrongPassword() throws Exception {
        String token = signup("bye2", "안녕히");

        assertThat(withdraw(token, "wrong-password").getResponse().getStatus()).isEqualTo(401);

        // 그대로 쓸 수 있어야 한다
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("혼자 쓰던 여행은 탈퇴할 때 같이 삭제된다 (넘길 사람이 없으므로)")
    void soloTripIsDeleted() throws Exception {
        String token = signup("bye3", "주인장");

        MvcResult trip = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"혼자 간 여행\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number tripId = JsonPath.read(trip.getResponse().getContentAsString(), "$.id");

        assertThat(withdraw(token, "pass1234").getResponse().getStatus()).isEqualTo(204);

        // 넘길 사람이 없었으므로 여행도 함께 지워진다 (소프트 삭제)
        assertThat(tripRepository.findByIdAndDeletedAtIsNull(tripId.longValue())).isEmpty();
    }

    @Test
    @DisplayName("주인이 탈퇴하면 닉네임 가나다 순 첫 번째 팀원에게 주인이 넘어간다")
    void ownerIsHandedOverOnWithdraw() throws Exception {
        String owner = signup("owner3b", "주인장");
        signup("mate3b1", "하늘");
        signup("mate3b2", "가람");

        MvcResult trip = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"같이 간 여행\",\"inviteeUsernames\":[\"mate3b1\",\"mate3b2\"]}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number tripId = JsonPath.read(trip.getResponse().getContentAsString(), "$.id");

        String sky = acceptInvitation("mate3b1");
        String garam = acceptInvitation("mate3b2");

        assertThat(withdraw(owner, "pass1234").getResponse().getStatus()).isEqualTo(204);

        // "가람" 이 "하늘" 보다 앞서므로 가람이 새 주인이 된다
        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + garam))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("OWNER"));
        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + sky))
                .andExpect(jsonPath("$.myRole").value("MEMBER"));

        // 새 주인은 주인만 할 수 있는 일(수정)을 할 수 있다
        mockMvc.perform(patch("/trips/" + tripId)
                        .header("Authorization", "Bearer " + garam)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"이름 바꿈\"}"))
                .andExpect(status().isOk());
    }

    /** 받은 초대를 수락하고 토큰을 돌려준다. */
    private String acceptInvitation(String username) throws Exception {
        String token = login(username);
        MvcResult invitations = mockMvc.perform(get("/trip-invitations")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        Number invitationId = JsonPath.read(invitations.getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return token;
    }

    @Test
    @DisplayName("탈퇴해도 팀원 여행에 남긴 기록은 지워지지 않는다")
    void recordsSurvive() throws Exception {
        String owner = signup("owner4", "주인장");
        String mate = signup("mate4", "떠날사람");

        // 팀원으로 합류
        MvcResult trip = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"같이 간 여행\",\"inviteeUsernames\":[\"mate4\"]}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number tripId = JsonPath.read(trip.getResponse().getContentAsString(), "$.id");

        MvcResult invitations = mockMvc.perform(get("/trip-invitations")
                        .header("Authorization", "Bearer " + mate))
                .andReturn();
        Number invitationId = JsonPath.read(invitations.getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk());

        // 핀 하나에 기록을 남긴다
        MvcResult day = mockMvc.perform(post("/itinerary-days")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":1}", tripId)))
                .andExpect(status().isCreated())
                .andReturn();
        Number dayId = JsonPath.read(day.getResponse().getContentAsString(), "$.id");

        MvcResult pin = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"tripId\":%d,\"dayId\":%d,\"place\":{\"name\":\"협재해수욕장\",\"latitude\":33.39,\"longitude\":126.23}}",
                                tripId, dayId)))
                .andExpect(status().isCreated())
                .andReturn();
        Number pinId = JsonPath.read(pin.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/diaries")
                        .header("Authorization", "Bearer " + mate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"itineraryItemId\":%d,\"content\":\"떠나기 전 기록\"}", pinId)))
                .andExpect(status().isCreated());

        assertThat(withdraw(mate, "pass1234").getResponse().getStatus()).isEqualTo(204);

        // 남은 팀원에게는 기록이 그대로 보이고, 작성자는 "탈퇴한 사용자" 로 표시된다
        mockMvc.perform(get("/diaries").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("떠나기 전 기록"))
                .andExpect(jsonPath("$[0].author.nickname").value("탈퇴한 사용자"));
    }

    @Test
    @DisplayName("탈퇴하면 팀원 검색에 더 이상 나오지 않고, 같은 이메일로 다시 가입할 수 있다")
    void afterWithdraw() throws Exception {
        String leaving = signup("bye5", "사라질사람");
        String staying = signup("stay5", "남는사람");

        mockMvc.perform(get("/users/search").param("keyword", "bye5")
                        .header("Authorization", "Bearer " + staying))
                .andExpect(jsonPath("$.length()").value(1));

        assertThat(withdraw(leaving, "pass1234").getResponse().getStatus()).isEqualTo(204);

        mockMvc.perform(get("/users/search").param("keyword", "bye5")
                        .header("Authorization", "Bearer " + staying))
                .andExpect(jsonPath("$.length()").value(0));

        // 이메일을 비워뒀으므로 같은 이메일로 새로 가입할 수 있다 (아이디는 그 사람 것으로 남겨둔다)
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bye5@pinkok.com\",\"username\":\"bye5again\",\"password\":\"pass1234\",\"nickname\":\"돌아옴\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("비밀번호 없이 보내면 400, 토큰 없으면 401")
    void invalidRequests() throws Exception {
        String token = signup("bye6", "안녕히");

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"pass1234\"}"))
                .andExpect(status().isUnauthorized());
    }
}
