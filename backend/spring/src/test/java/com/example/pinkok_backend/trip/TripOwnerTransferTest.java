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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 여행 주인 넘기기.
 *
 * <p>주인만 할 수 있는 일(수정·삭제·초대)이 있어서, 주인이 빠질 때 자리를 이어받을 사람이 필요하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripOwnerTransferTest {

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

    private Long userIdOf(String token) throws Exception {
        MvcResult me = mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(me.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long createTripWith(String ownerToken, String inviteeUsername) throws Exception {
        MvcResult trip = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"title\":\"같이 간 여행\",\"inviteeUsernames\":[\"%s\"]}",
                                inviteeUsername)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(trip.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void acceptInvitation(String token) throws Exception {
        MvcResult invitations = mockMvc.perform(get("/trip-invitations")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        Number invitationId = JsonPath.read(invitations.getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(post("/trip-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private MvcResult transfer(String token, Long tripId, Long newOwnerUserId) throws Exception {
        return mockMvc.perform(patch("/trips/" + tripId + "/owner")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"newOwnerUserId\":%d}", newOwnerUserId)))
                .andReturn();
    }

    @Test
    @DisplayName("주인을 넘기면 역할이 서로 바뀌고, 넘겨받은 사람만 여행을 수정할 수 있다")
    void transferOwner() throws Exception {
        String owner = signupAndLogin("owner1", "주인장");
        String mate = signupAndLogin("mate1", "팀원");

        Long tripId = createTripWith(owner, "mate1");
        acceptInvitation(mate);

        transfer(owner, tripId, userIdOf(mate));

        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + mate))
                .andExpect(jsonPath("$.myRole").value("OWNER"));
        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.myRole").value("MEMBER"));

        // 새 주인은 수정할 수 있고
        mockMvc.perform(patch("/trips/" + tripId)
                        .header("Authorization", "Bearer " + mate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"새 주인이 바꾼 이름\"}"))
                .andExpect(status().isOk());

        // 넘긴 사람은 더 이상 못 한다
        mockMvc.perform(patch("/trips/" + tripId)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"되돌리기\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("팀원이 아닌 사람에게는 넘길 수 없고(400), 주인이 아니면 넘길 수 없다(403)")
    void invalidTransfer() throws Exception {
        String owner = signupAndLogin("owner2", "주인장");
        String mate = signupAndLogin("mate2", "팀원");
        String stranger = signupAndLogin("stranger2", "남남이");

        Long tripId = createTripWith(owner, "mate2");
        acceptInvitation(mate);

        // 아직 팀원이 아닌 사람
        org.assertj.core.api.Assertions.assertThat(
                transfer(owner, tripId, userIdOf(stranger)).getResponse().getStatus()).isEqualTo(400);

        // 자기 자신
        org.assertj.core.api.Assertions.assertThat(
                transfer(owner, tripId, userIdOf(owner)).getResponse().getStatus()).isEqualTo(400);

        // 주인이 아닌 팀원이 시도
        org.assertj.core.api.Assertions.assertThat(
                transfer(mate, tripId, userIdOf(owner)).getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("토큰 없으면 401")
    void withoutToken() throws Exception {
        mockMvc.perform(patch("/trips/1/owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOwnerUserId\":2}"))
                .andExpect(status().isUnauthorized());
    }
}
