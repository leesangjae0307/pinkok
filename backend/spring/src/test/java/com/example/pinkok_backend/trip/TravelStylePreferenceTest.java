package com.example.pinkok_backend.trip;

import com.example.pinkok_backend.seed.TravelStyleSeeder;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 사용자·여행별 여행 취향 저장 (통째로 교체, 팀원 권한, 잘못된 코드). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TravelStylePreferenceTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TravelStyleSeeder travelStyleSeeder;

    @BeforeEach
    void seedStyles() {
        travelStyleSeeder.seed();
    }

    private String signupAndLogin(String email, String username) throws Exception {
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"닉네임\"}",
                                email, username)))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"pass1234\"}", email)))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private long createTrip(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제주\"}"))
                .andExpect(status().isCreated()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void putUserStyles(String token, String codesJson) throws Exception {
        mockMvc.perform(put("/user-travel-styles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"styleCodes\":" + codesJson + "}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 취향 - 처음엔 비어 있고, 저장하면 조회된다")
    void userStyles_saveAndList() throws Exception {
        String token = signupAndLogin("style1@pinkok.com", "styleuser1");

        mockMvc.perform(get("/user-travel-styles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/user-travel-styles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"styleCodes\":[\"FOOD\",\"CAFE\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("FOOD"))
                .andExpect(jsonPath("$[1].code").value("CAFE"));

        mockMvc.perform(get("/user-travel-styles").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("맛집 위주"));
    }

    @Test
    @DisplayName("내 취향 - 통째로 교체된다 (빠진 것은 해제, 그대로인 것은 유지, 빈 배열이면 전부 해제)")
    void userStyles_replaceSemantics() throws Exception {
        String token = signupAndLogin("style2@pinkok.com", "styleuser2");
        putUserStyles(token, "[\"FOOD\",\"CAFE\"]");

        putUserStyles(token, "[\"CAFE\",\"NATURE\"]");
        mockMvc.perform(get("/user-travel-styles").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.code=='CAFE')]").exists())
                .andExpect(jsonPath("$[?(@.code=='NATURE')]").exists())
                .andExpect(jsonPath("$[?(@.code=='FOOD')]").doesNotExist());

        putUserStyles(token, "[]");
        mockMvc.perform(get("/user-travel-styles").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("코드는 소문자·공백·중복이 섞여도 정리된다")
    void userStyles_normalizesCodes() throws Exception {
        String token = signupAndLogin("style3@pinkok.com", "styleuser3");

        mockMvc.perform(put("/user-travel-styles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"styleCodes\":[\" food \",\"FOOD\",\"\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("FOOD"));
    }

    @Test
    @DisplayName("모르는 코드가 있으면 400이고, 아무것도 바뀌지 않는다")
    void unknownCode_returns400_andKeepsPrevious() throws Exception {
        String token = signupAndLogin("style4@pinkok.com", "styleuser4");
        putUserStyles(token, "[\"FOOD\"]");

        mockMvc.perform(put("/user-travel-styles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"styleCodes\":[\"CAFE\",\"NOPE\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NOPE")));

        mockMvc.perform(get("/user-travel-styles").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("FOOD"));
    }

    @Test
    @DisplayName("styleCodes 가 없으면 400, 토큰이 없으면 401")
    void validationAndAuth() throws Exception {
        String token = signupAndLogin("style5@pinkok.com", "styleuser5");

        mockMvc.perform(put("/user-travel-styles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/user-travel-styles")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/user-travel-styles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"styleCodes\":[]}")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 취향은 다른 사람과 섞이지 않는다")
    void userStyles_areIsolatedPerUser() throws Exception {
        String a = signupAndLogin("style6a@pinkok.com", "styleuser6a");
        String b = signupAndLogin("style6b@pinkok.com", "styleuser6b");
        putUserStyles(a, "[\"FOOD\"]");
        putUserStyles(b, "[\"NATURE\",\"SHOPPING\"]");

        mockMvc.perform(get("/user-travel-styles").header("Authorization", "Bearer " + a))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/user-travel-styles").header("Authorization", "Bearer " + b))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("여행 취향 - 저장·조회·교체")
    void tripStyles_saveListReplace() throws Exception {
        String token = signupAndLogin("style7@pinkok.com", "styleuser7");
        long tripId = createTrip(token);

        mockMvc.perform(get("/trip-travel-styles").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/trip-travel-styles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"styleCodes\":[\"SIGHT\",\"FOOD\"]}", tripId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(put("/trip-travel-styles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"styleCodes\":[\"FOOD\"]}", tripId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/trip-travel-styles").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("FOOD"));
    }

    @Test
    @DisplayName("여행 취향 - 팀원이 아니면 조회·변경 모두 403, 없는 여행이면 403/404")
    void tripStyles_requireMembership() throws Exception {
        String owner = signupAndLogin("style8@pinkok.com", "styleuser8");
        String stranger = signupAndLogin("style8b@pinkok.com", "styleuser8b");
        long tripId = createTrip(owner);

        mockMvc.perform(get("/trip-travel-styles").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/trip-travel-styles").header("Authorization", "Bearer " + stranger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"styleCodes\":[\"FOOD\"]}", tripId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/trip-travel-styles").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"styleCodes\":[]}"))
                .andExpect(status().isBadRequest()); // tripId 없음
    }
}
