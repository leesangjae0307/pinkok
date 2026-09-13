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
 * 일자(itinerary_days) · 핀(itinerary_items) CRUD, 순서 재배치, 여행 팀원 권한 확인.
 * PLAN.md 완료 기준: "여행을 만들고 -> 핀을 꽂고 -> ... 흐름이 끝까지 되고,
 * 팀원이 아닌 사람은 남의 여행을 못 본다."
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ItineraryTest {

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

    private Long createTrip(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제주도 여행\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private Long createDay(String token, Long tripId, int dayNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/itinerary-days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":%d}", tripId, dayNumber)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static String placeJson(String name) {
        return String.format(
                "{\"name\":\"%s\",\"latitude\":33.450701,\"longitude\":126.570667}", name);
    }

    private Long addItem(String token, Long tripId, Long dayId, String placeName) throws Exception {
        String dayPart = dayId == null ? "" : String.format("\"dayId\":%d,", dayId);
        String body = String.format("{\"tripId\":%d,%s\"place\":%s}", tripId, dayPart, placeJson(placeName));
        MvcResult result = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    @DisplayName("일자 생성 - 같은 일차 번호 중복이면 409")
    void createDay_duplicateDayNumber_returns409() throws Exception {
        String token = signupAndLogin("dayowner@pinkok.com", "dayowner01");
        Long tripId = createTrip(token);
        createDay(token, tripId, 1);

        mockMvc.perform(post("/itinerary-days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":1}", tripId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("일자 생성 - 팀원이 아니면 403")
    void createDay_forbiddenForNonMember() throws Exception {
        String ownerToken = signupAndLogin("dayowner2@pinkok.com", "dayowner2");
        Long tripId = createTrip(ownerToken);

        String strangerToken = signupAndLogin("daystranger@pinkok.com", "daystranger");
        mockMvc.perform(post("/itinerary-days")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":1}", tripId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("핀 추가 - 같은 카카오 장소 id면 장소를 재사용한다")
    void addItem_reusesPlaceByKakaoId() throws Exception {
        String token = signupAndLogin("pinowner@pinkok.com", "pinowner01");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);

        String bodyWithKakaoId =
                "{\"tripId\":" + tripId + ",\"dayId\":" + dayId
                        + ",\"place\":{\"kakaoPlaceId\":\"KAKAO-1\",\"name\":\"성산일출봉\",\"latitude\":33.4587,\"longitude\":126.9425}}";

        MvcResult first = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithKakaoId))
                .andExpect(status().isCreated())
                .andReturn();
        Number placeId1 = JsonPath.read(first.getResponse().getContentAsString(), "$.place.id");

        MvcResult second = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithKakaoId))
                .andExpect(status().isCreated())
                .andReturn();
        Number placeId2 = JsonPath.read(second.getResponse().getContentAsString(), "$.place.id");

        org.junit.jupiter.api.Assertions.assertEquals(placeId1, placeId2);
    }

    @Test
    @DisplayName("핀 추가 - 날짜 미배정으로도 추가할 수 있고, 순서는 null")
    void addItem_withoutDay() throws Exception {
        String token = signupAndLogin("pinowner2@pinkok.com", "pinowner02");
        Long tripId = createTrip(token);

        mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"place\":%s}", tripId, placeJson("한라산"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayId").doesNotExist())
                .andExpect(jsonPath("$.visitOrder").doesNotExist());
    }

    @Test
    @DisplayName("핀 목록 - 팀원이 아니면 403, 팀원이면 순서대로 조회")
    void listItems_orderedAndProtected() throws Exception {
        String token = signupAndLogin("pinowner3@pinkok.com", "pinowner03");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        addItem(token, tripId, dayId, "첫번째");
        addItem(token, tripId, dayId, "두번째");

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].place.name").value("첫번째"))
                .andExpect(jsonPath("$[0].visitOrder").value(1))
                .andExpect(jsonPath("$[1].visitOrder").value(2));

        String strangerToken = signupAndLogin("pinstranger@pinkok.com", "pinstranger");
        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("핀 순서 재배치 - itemIds 순서대로 1부터 다시 매겨진다")
    void reorderItems() throws Exception {
        String token = signupAndLogin("reorderer@pinkok.com", "reorderer01");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        Long item1 = addItem(token, tripId, dayId, "A");
        Long item2 = addItem(token, tripId, dayId, "B");
        Long item3 = addItem(token, tripId, dayId, "C");

        String reorderBody = String.format(
                "{\"dayId\":%d,\"itemIds\":[%d,%d,%d]}", dayId, item3, item1, item2);

        mockMvc.perform(patch("/itinerary-items/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reorderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(item3))
                .andExpect(jsonPath("$[0].visitOrder").value(1))
                .andExpect(jsonPath("$[1].id").value(item1))
                .andExpect(jsonPath("$[1].visitOrder").value(2))
                .andExpect(jsonPath("$[2].id").value(item2))
                .andExpect(jsonPath("$[2].visitOrder").value(3));
    }

    @Test
    @DisplayName("핀 수정 - 이동수단 · 메모 변경, 다른 여행의 일자로는 못 옮긴다")
    void updateItem() throws Exception {
        String token = signupAndLogin("updater@pinkok.com", "updater01");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        Long itemId = addItem(token, tripId, dayId, "장소");

        mockMvc.perform(patch("/itinerary-items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transportMode\":\"WALK\",\"memo\":\"도보 이동\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transportMode").value("WALK"))
                .andExpect(jsonPath("$.memo").value("도보 이동"));

        Long otherTripId = createTrip(token);
        Long otherDayId = createDay(token, otherTripId, 1);
        mockMvc.perform(patch("/itinerary-items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"dayId\":%d}", otherDayId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("일자 삭제 - 그 핀은 지워지지 않고 날짜 미배정으로 남는다")
    void deleteDay_unassignsItemsInstead() throws Exception {
        String token = signupAndLogin("daydeleter@pinkok.com", "daydeleter1");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        Long itemId = addItem(token, tripId, dayId, "남을 핀");

        mockMvc.perform(delete("/itinerary-days/" + dayId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemId))
                .andExpect(jsonPath("$[0].dayId").doesNotExist());
    }

    @Test
    @DisplayName("완료 기준 흐름 - 여행 만들고 -> 일자 만들고 -> 핀 꽂는다")
    void endToEndFlow() throws Exception {
        String token = signupAndLogin("e2e@pinkok.com", "e2euser01");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        Long itemId = addItem(token, tripId, dayId, "협재 해수욕장");

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemId))
                .andExpect(jsonPath("$[0].place.name").value("협재 해수욕장"));
    }
}
