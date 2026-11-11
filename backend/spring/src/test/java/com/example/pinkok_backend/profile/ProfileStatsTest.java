package com.example.pinkok_backend.profile;

import com.example.pinkok_backend.seed.TravelStyleSeeder;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프론트 요청 3건.
 * <ul>
 *   <li>PATCH /itinerary-items/{id} — dayId / plannedArrivalTime 에 null 을 보내면 해제</li>
 *   <li>GET /users/me/stats — 프로필 집계를 한 번에</li>
 *   <li>GET /pinlogs?mine=true — 내 여행들의 완성된 PinLog 를 한 번에</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileStatsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TravelStyleSeeder travelStyleSeeder;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        travelStyleSeeder.seed();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    // ---------- 준비용 도우미 ----------

    private String signupAndLogin(String username) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s@pinkok.com\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"%s\"}",
                                username, username, username)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s@pinkok.com\",\"password\":\"pass1234\"}", username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private Long createTrip(String token, String title, String region) throws Exception {
        String regionPart = region == null ? "" : String.format(",\"region\":\"%s\"", region);
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"title\":\"%s\"%s}", title, regionPart)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long createDay(String token, Long tripId, int dayNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/itinerary-days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":%d}", tripId, dayNumber)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long addPin(String token, Long tripId, Long dayId, String placeName) throws Exception {
        String dayPart = dayId == null ? "" : String.format("\"dayId\":%d,", dayId);
        MvcResult result = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"tripId\":%d,%s\"place\":{\"name\":\"%s\",\"latitude\":33.45,\"longitude\":126.57}}",
                                tripId, dayPart, placeName)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void writeDiary(String token, Long pinId) throws Exception {
        mockMvc.perform(post("/diaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"itineraryItemId\":%d,\"content\":\"좋았다\",\"rating\":5,\"media\":[]}", pinId)))
                .andExpect(status().isCreated());
    }

    private void setStyles(String token, Long tripId, String codesJson) throws Exception {
        mockMvc.perform(put("/trip-travel-styles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"styleCodes\":%s}", tripId, codesJson)))
                .andExpect(status().isOk());
    }

    private Long userIdOf(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    /** 합성은 오래 걸려서 테스트에서는 결과 상태의 PinLog 를 바로 넣는다. */
    private void insertPinlog(Long tripId, String username, String title, String status) {
        jdbcTemplate.update(
                "INSERT INTO pinlogs (trip_id, user_id, title, status, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                tripId, userIdOf(username), title, status);
    }

    private String getJson(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------- PATCH null 해제 ----------

    @Test
    @DisplayName("핀 수정 - dayId·plannedArrivalTime 에 null 을 보내면 해제, 안 보내면 그대로")
    void patchNullClears() throws Exception {
        String token = signupAndLogin("gamjieun");
        Long tripId = createTrip(token, "제주도 여행", "제주");
        Long dayId = createDay(token, tripId, 1);
        Long pinId = addPin(token, tripId, dayId, "협재해수욕장");

        mockMvc.perform(patch("/itinerary-items/" + pinId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedArrivalTime\":\"10:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedArrivalTime").value("10:30:00"));

        // 다른 필드만 보내면 일자·시각은 그대로
        mockMvc.perform(patch("/itinerary-items/" + pinId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"노을 보기\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayId").value(dayId))
                .andExpect(jsonPath("$.plannedArrivalTime").value("10:30:00"));

        // null 을 보내면 해제
        mockMvc.perform(patch("/itinerary-items/" + pinId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayId\":null,\"plannedArrivalTime\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayId").doesNotExist())
                .andExpect(jsonPath("$.visitOrder").doesNotExist())
                .andExpect(jsonPath("$.plannedArrivalTime").doesNotExist())
                .andExpect(jsonPath("$.memo").value("노을 보기"));

        // 다시 날짜에 넣으면 그 날짜 맨 뒤로
        mockMvc.perform(patch("/itinerary-items/" + pinId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"dayId\":%d}", dayId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayId").value(dayId))
                .andExpect(jsonPath("$.visitOrder").value(1));
    }

    // ---------- 프로필 집계 ----------

    @Test
    @DisplayName("프로필 집계 - 방문(내 기록)·저장(핀)·지역·취향을 한 번에 돌려준다")
    void stats() throws Exception {
        String me = signupAndLogin("gamjieun");
        String mate = signupAndLogin("jeonghun");

        Long jeju1 = createTrip(me, "제주 1", "제주");
        Long jeju2 = createTrip(me, "제주 2", "제주");
        Long busan = createTrip(me, "부산", "부산");
        createTrip(me, "지역 없음", null);

        Long p1 = addPin(me, jeju1, null, "협재해수욕장");
        Long p2 = addPin(me, jeju1, null, "성산일출봉");
        addPin(me, jeju2, null, "우도");
        Long p4 = addPin(me, busan, null, "해운대");

        writeDiary(me, p1);
        writeDiary(me, p4);

        setStyles(me, jeju1, "[\"FOOD\",\"CAFE\"]");
        setStyles(me, jeju2, "[\"FOOD\"]");
        setStyles(me, busan, "[\"NATURE\"]");

        // 남의 여행·남의 기록은 섞이지 않는다
        Long mateTrip = createTrip(mate, "남의 여행", "서울");
        addPin(mate, mateTrip, null, "경복궁");
        // 내 여행이라도 남이 쓴 기록은 "내 방문"이 아니다 → 팀원으로 넣고 기록을 쓰게 한다
        jdbcTemplate.update("INSERT INTO trip_members (trip_id, user_id, role, joined_at) VALUES (?, ?, 'MEMBER', CURRENT_TIMESTAMP)",
                jeju1, userIdOf("jeonghun"));
        writeDiary(mate, p2);

        String json = getJson(me, "/users/me/stats");

        assertThat((Integer) JsonPath.read(json, "$.savedCount")).isEqualTo(4);
        assertThat((Integer) JsonPath.read(json, "$.visitedCount")).isEqualTo(2);
        assertThat((List<String>) JsonPath.read(json, "$.visited[*].placeName"))
                .containsExactlyInAnyOrder("협재해수욕장", "해운대");
        assertThat((List<String>) JsonPath.read(json, "$.visited[?(@.placeName=='해운대')].tripTitle"))
                .containsExactly("부산");

        // 지역: 많은 순 (제주 2, 부산 1), 지역 없는 여행은 빠진다
        assertThat((Integer) JsonPath.read(json, "$.regionCount")).isEqualTo(2);
        assertThat((String) JsonPath.read(json, "$.regions[0].name")).isEqualTo("제주");
        assertThat((Integer) JsonPath.read(json, "$.regions[0].tripCount")).isEqualTo(2);

        // 취향: FOOD 2 / CAFE 1 / NATURE 1 → 50% / 25% / 25%
        assertThat((String) JsonPath.read(json, "$.tastes[0].code")).isEqualTo("FOOD");
        assertThat((Integer) JsonPath.read(json, "$.tastes[0].count")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(json, "$.tastes[0].percent")).isEqualTo(50);
        assertThat((List<?>) JsonPath.read(json, "$.tastes")).hasSize(3);
    }

    @Test
    @DisplayName("프로필 집계 - 여행이 없으면 모두 0 과 빈 목록, 로그인 안 하면 401")
    void statsEmpty() throws Exception {
        String me = signupAndLogin("gamjieun");
        String json = getJson(me, "/users/me/stats");
        assertThat((Integer) JsonPath.read(json, "$.savedCount")).isZero();
        assertThat((List<?>) JsonPath.read(json, "$.visited")).isEmpty();
        assertThat((List<?>) JsonPath.read(json, "$.tastes")).isEmpty();

        mockMvc.perform(get("/users/me/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("프로필 집계 - 삭제한 여행은 세지 않는다")
    void statsSkipsDeletedTrip() throws Exception {
        String me = signupAndLogin("gamjieun");
        Long tripId = createTrip(me, "지울 여행", "제주");
        addPin(me, tripId, null, "협재해수욕장");

        mockMvc.perform(delete("/trips/" + tripId).header("Authorization", "Bearer " + me))
                .andExpect(status().is2xxSuccessful());

        String json = getJson(me, "/users/me/stats");
        assertThat((Integer) JsonPath.read(json, "$.savedCount")).isZero();
        assertThat((Integer) JsonPath.read(json, "$.regionCount")).isZero();
    }

    // ---------- 내 PinLog 모아보기 ----------

    @Test
    @DisplayName("GET /pinlogs?mine=true - 내 여행들의 완성된 PinLog 만, 남의 여행 것은 빠진다")
    void myPinlogs() throws Exception {
        String me = signupAndLogin("gamjieun");
        String mate = signupAndLogin("jeonghun");
        Long jeju = createTrip(me, "제주", "제주");
        Long busan = createTrip(me, "부산", "부산");
        Long others = createTrip(mate, "남의 여행", "서울");

        insertPinlog(jeju, "gamjieun", "제주 완성", "DONE");
        insertPinlog(busan, "gamjieun", "부산 완성", "DONE");
        insertPinlog(busan, "gamjieun", "부산 실패", "FAILED");
        insertPinlog(others, "jeonghun", "남의 완성", "DONE");

        String json = getJson(me, "/pinlogs?mine=true");
        assertThat((List<String>) JsonPath.read(json, "$[*].title"))
                .containsExactlyInAnyOrder("제주 완성", "부산 완성");

        // tripId 도 mine 도 없으면 400
        mockMvc.perform(get("/pinlogs").header("Authorization", "Bearer " + me))
                .andExpect(status().isBadRequest());
        // 원래 쓰던 tripId 방식은 그대로 (실패한 것도 보인다)
        assertThat((List<?>) JsonPath.read(getJson(me, "/pinlogs?tripId=" + busan), "$")).hasSize(2);
    }
}
