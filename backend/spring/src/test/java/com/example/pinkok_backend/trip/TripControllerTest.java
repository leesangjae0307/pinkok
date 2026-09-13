package com.example.pinkok_backend.trip;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 여행 카드 CRUD + "팀원이 아니면 못 본다" 권한 확인 테스트.
 * PLAN.md 완료 기준: "팀원이 아닌 사람은 남의 여행을 못 본다."
 *
 * <p>{@code @Transactional} 로 각 테스트를 롤백시킨다 — users/trips/trip_members 는
 * FK 로 얽혀 있어서 deleteAll() 순서를 일일이 맞추는 것보다 안전하고,
 * 다른 테스트 클래스(H2를 같이 쓰는)에 데이터가 새어나가지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripControllerTest {

    @Autowired
    MockMvc mockMvc;

    private String signupAndLogin(String email, String username) throws Exception {
        String signupBody = String.format(
                "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"닉네임\"}",
                email, username);
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        String loginBody = String.format("{\"email\":\"%s\",\"password\":\"pass1234\"}", email);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private Long createTrip(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"title\":\"%s\"}", title)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    @DisplayName("여행 생성 - 만든 사람은 자동으로 OWNER")
    void create_makesCreatorOwner() throws Exception {
        String token = signupAndLogin("owner@pinkok.com", "owneruser");

        mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제주도 여행\",\"region\":\"제주\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("제주도 여행"))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.myRole").value("OWNER"));
    }

    @Test
    @DisplayName("여행 목록 - 내가 만든 것만 보인다")
    void list_onlyMine() throws Exception {
        String token = signupAndLogin("lister@pinkok.com", "listeruser");
        createTrip(token, "여행 A");
        createTrip(token, "여행 B");

        mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("여행 상세 - 팀원이 아니면 403")
    void get_forbiddenForNonMember() throws Exception {
        String ownerToken = signupAndLogin("tripowner@pinkok.com", "tripowner01");
        Long tripId = createTrip(ownerToken, "우리끼리 여행");

        String strangerToken = signupAndLogin("stranger@pinkok.com", "stranger01");

        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("여행 상세 - OWNER 본인은 조회 가능")
    void get_allowedForOwner() throws Exception {
        String token = signupAndLogin("owner2@pinkok.com", "owner2user");
        Long tripId = createTrip(token, "혼자 여행");

        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("혼자 여행"));
    }

    @Test
    @DisplayName("여행 수정 - 팀원이 아니면 403, OWNER면 200")
    void update_ownerOnly() throws Exception {
        String ownerToken = signupAndLogin("owner3@pinkok.com", "owner3user");
        Long tripId = createTrip(ownerToken, "원래 제목");

        String strangerToken = signupAndLogin("stranger2@pinkok.com", "stranger2user");
        mockMvc.perform(patch("/trips/" + tripId)
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"남의 여행 해킹\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/trips/" + tripId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"바뀐 제목\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목"));
    }

    @Test
    @DisplayName("여행 삭제 - OWNER만 가능하고, 삭제 후에는 목록에서 사라진다")
    void delete_ownerOnly_andSoftDeletes() throws Exception {
        String token = signupAndLogin("owner4@pinkok.com", "owner4user");
        Long tripId = createTrip(token, "삭제될 여행");

        mockMvc.perform(delete("/trips/" + tripId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("로그인 없이 여행 API 호출하면 401")
    void requiresAuth() throws Exception {
        mockMvc.perform(get("/trips")).andExpect(status().isUnauthorized());
    }
}
