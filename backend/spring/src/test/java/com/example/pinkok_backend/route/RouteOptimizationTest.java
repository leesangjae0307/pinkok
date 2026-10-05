package com.example.pinkok_backend.route;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 동선 최적화 API: 제안 생성 → 조회 → 일정 반영, 권한·오래된 제안 처리. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RouteOptimizationTest {

    @Autowired
    MockMvc mockMvc;

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

    private long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long createTrip(String token) throws Exception {
        return idOf(mockMvc.perform(post("/trips").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제주\"}"))
                .andExpect(status().isCreated()).andReturn());
    }

    private long createDay(String token, long tripId, int dayNumber) throws Exception {
        return idOf(mockMvc.perform(post("/itinerary-days").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":%d}", tripId, dayNumber)))
                .andExpect(status().isCreated()).andReturn());
    }

    private long addItem(String token, long tripId, Long dayId, String name, double lat, double lng) throws Exception {
        String dayPart = dayId == null ? "" : String.format("\"dayId\":%d,", dayId);
        String body = String.format("{\"tripId\":%d,%s\"place\":{\"name\":\"%s\",\"latitude\":%s,\"longitude\":%s}}",
                tripId, dayPart, name, lat, lng);
        return idOf(mockMvc.perform(post("/itinerary-items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn());
    }

    private MvcResult optimize(String token, long tripId, String extraJson) throws Exception {
        return mockMvc.perform(post("/route-optimizations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d%s}", tripId, extraJson)))
                .andExpect(status().isCreated()).andReturn();
    }

    /** 제주 서쪽(애월) 3곳 + 동쪽(성산) 3곳을 일부러 날짜마다 섞어서 꽂는다. */
    private long[] zigzagTrip(String token, long tripId, long day1, long day2) throws Exception {
        return new long[]{
                addItem(token, tripId, day1, "서1", 33.460, 126.310),
                addItem(token, tripId, day1, "동1", 33.460, 126.930),
                addItem(token, tripId, day1, "서2", 33.465, 126.320),
                addItem(token, tripId, day2, "동2", 33.455, 126.940),
                addItem(token, tripId, day2, "서3", 33.470, 126.330),
                addItem(token, tripId, day2, "동3", 33.450, 126.950)};
    }

    @Test
    @DisplayName("제안 생성 - 같은 지역끼리 같은 날로 모이고, 지금 일정보다 거리가 줄어든다 (일정은 아직 그대로)")
    void create_groupsNearbyPinsAndShortensDistance() throws Exception {
        String token = signupAndLogin("route1@pinkok.com", "routeuser1");
        long tripId = createTrip(token);
        long day1 = createDay(token, tripId, 1);
        long day2 = createDay(token, tripId, 2);
        zigzagTrip(token, tripId, day1, day2);

        MvcResult result = optimize(token, tripId, "");
        String body = result.getResponse().getContentAsString();

        assertEquals(2, ((List<?>) JsonPath.read(body, "$.days")).size());
        List<String> d1 = JsonPath.read(body, "$.days[0].stops[*].placeName");
        List<String> d2 = JsonPath.read(body, "$.days[1].stops[*].placeName");
        assertEquals(3, d1.size());
        assertEquals(3, d2.size());
        // 한 날은 전부 "서", 다른 날은 전부 "동"
        assertTrue(d1.stream().allMatch(n -> n.startsWith("서")) || d1.stream().allMatch(n -> n.startsWith("동")), "1일차: " + d1);
        assertTrue(d2.stream().allMatch(n -> n.startsWith("서")) || d2.stream().allMatch(n -> n.startsWith("동")), "2일차: " + d2);
        assertTrue(d1.get(0).charAt(0) != d2.get(0).charAt(0));

        int before = JsonPath.read(body, "$.beforeDistanceM");
        int after = JsonPath.read(body, "$.totalDistanceM");
        assertTrue(after < before, "before=" + before + " after=" + after);

        // 그날 첫 장소는 이동 정보 없음, 이후는 있음
        assertEquals(0, (int) JsonPath.read(body, "$.days[0].stops[0].distanceFromPreviousM"));
        assertTrue((int) JsonPath.read(body, "$.days[0].stops[1].distanceFromPreviousM") > 0);
        assertEquals(false, JsonPath.read(body, "$.applied"));

        // 아직 반영 전이므로 일정은 처음 그대로 (1일차에 서1·동1·서2)
        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].place.name").value("서1"))
                .andExpect(jsonPath("$[1].place.name").value("동1"));
    }

    @Test
    @DisplayName("반영 - 핀의 날짜·순서·이동수단이 제안대로 바뀌고, 다시 반영하면 409")
    void apply_rewritesItinerary() throws Exception {
        String token = signupAndLogin("route2@pinkok.com", "routeuser2");
        long tripId = createTrip(token);
        long day1 = createDay(token, tripId, 1);
        long day2 = createDay(token, tripId, 2);
        zigzagTrip(token, tripId, day1, day2);

        MvcResult created = optimize(token, tripId, "");
        long optId = idOf(created);
        List<String> expectedDay1 = JsonPath.read(created.getResponse().getContentAsString(), "$.days[0].stops[*].placeName");
        List<String> expectedDay2 = JsonPath.read(created.getResponse().getContentAsString(), "$.days[1].stops[*].placeName");

        mockMvc.perform(post("/route-optimizations/" + optId + "/apply").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));

        MvcResult items = mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        String body = items.getResponse().getContentAsString();
        List<String> names = JsonPath.read(body, "$[*].place.name");
        List<Integer> orders = JsonPath.read(body, "$[*].visitOrder");
        List<Integer> dayNumbers = JsonPath.read(body, "$[*].dayNumber");

        // 정렬 순서 = 1일차 3개(순서 1,2,3) → 2일차 3개
        assertEquals(expectedDay1, names.subList(0, 3));
        assertEquals(expectedDay2, names.subList(3, 6));
        assertEquals(List.of(1, 2, 3, 1, 2, 3), orders);
        assertEquals(List.of(1, 1, 1, 2, 2, 2), dayNumbers);
        List<String> modes = JsonPath.read(body, "$[*].transportMode");
        assertEquals(null, modes.get(0)); // 그날 첫 장소는 이동수단 없음
        assertEquals("CAR", modes.get(1));

        mockMvc.perform(post("/route-optimizations/" + optId + "/apply").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("redistribute=false - 날짜는 그대로 두고 날짜 안 순서만 바꾼다, 날짜 미배정 핀은 건드리지 않는다")
    void reorderOnly_keepsDaysAndUnassigned() throws Exception {
        String token = signupAndLogin("route3@pinkok.com", "routeuser3");
        long tripId = createTrip(token);
        long day1 = createDay(token, tripId, 1);
        createDay(token, tripId, 2);
        addItem(token, tripId, day1, "먼곳", 33.460, 126.950);
        addItem(token, tripId, day1, "가까운곳A", 33.460, 126.310);
        addItem(token, tripId, day1, "가까운곳B", 33.462, 126.312);
        addItem(token, tripId, day1, "중간", 33.461, 126.311);
        long loose = addItem(token, tripId, null, "미배정", 33.5, 126.5);

        MvcResult created = optimize(token, tripId, ",\"redistribute\":false");
        String body = created.getResponse().getContentAsString();
        assertEquals(1, ((List<?>) JsonPath.read(body, "$.days")).size());
        assertEquals(1, (int) JsonPath.read(body, "$.days[0].dayNumber"));
        List<Number> untouched = JsonPath.read(body, "$.untouchedItemIds");
        assertEquals(List.of(loose), untouched.stream().map(Number::longValue).toList());
        List<String> order = JsonPath.read(body, "$.days[0].stops[*].placeName");
        assertEquals("먼곳", order.get(0).equals("먼곳") ? "먼곳" : order.get(order.size() - 1)); // 먼 곳은 끝에 있다

        mockMvc.perform(post("/route-optimizations/" + idOf(created) + "/apply").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("제안을 만든 뒤 핀이 바뀌면 반영은 409")
    void apply_staleProposal_returns409() throws Exception {
        String token = signupAndLogin("route4@pinkok.com", "routeuser4");
        long tripId = createTrip(token);
        long day1 = createDay(token, tripId, 1);
        createDay(token, tripId, 2);
        addItem(token, tripId, day1, "A", 33.46, 126.31);
        addItem(token, tripId, day1, "B", 33.47, 126.33);
        long optId = idOf(optimize(token, tripId, ""));

        addItem(token, tripId, day1, "새로 추가", 33.48, 126.35);

        mockMvc.perform(post("/route-optimizations/" + optId + "/apply").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("조회 - 목록(최신순)과 상세, 팀원이 아니면 403")
    void listAndGet_andAccessControl() throws Exception {
        String token = signupAndLogin("route5@pinkok.com", "routeuser5");
        String stranger = signupAndLogin("route5b@pinkok.com", "routeuser5b");
        long tripId = createTrip(token);
        long day1 = createDay(token, tripId, 1);
        addItem(token, tripId, day1, "A", 33.46, 126.31);
        long first = idOf(optimize(token, tripId, ""));
        long second = idOf(optimize(token, tripId, ""));

        mockMvc.perform(get("/route-optimizations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));
        mockMvc.perform(get("/route-optimizations/" + first).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].stops[0].placeName").value("A"));

        mockMvc.perform(get("/route-optimizations/" + first).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/route-optimizations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/route-optimizations/" + first + "/apply").header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/route-optimizations").header("Authorization", "Bearer " + stranger)
                        .contentType(MediaType.APPLICATION_JSON).content(String.format("{\"tripId\":%d}", tripId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("에러 - 일자가 없거나 핀이 없으면 400, tripId 없으면 400, 토큰 없으면 401")
    void validationErrors() throws Exception {
        String token = signupAndLogin("route6@pinkok.com", "routeuser6");
        long tripId = createTrip(token);

        mockMvc.perform(post("/route-optimizations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(String.format("{\"tripId\":%d}", tripId)))
                .andExpect(status().isBadRequest()); // 일자 없음

        createDay(token, tripId, 1);
        mockMvc.perform(post("/route-optimizations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(String.format("{\"tripId\":%d}", tripId)))
                .andExpect(status().isBadRequest()); // 핀 없음

        mockMvc.perform(post("/route-optimizations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/route-optimizations").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d}", tripId)))
                .andExpect(status().isUnauthorized());
    }
}
