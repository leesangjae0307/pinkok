package com.example.pinkok_backend.recommendation;

import com.example.pinkok_backend.ai.TestAsyncConfig;
import com.example.pinkok_backend.gemini.GeminiClient;
import com.example.pinkok_backend.gemini.GeminiGenerateRequest;
import com.example.pinkok_backend.kakao.KakaoKeywordSearchResponse;
import com.example.pinkok_backend.kakao.KakaoLocalApiClient;
import com.example.pinkok_backend.seed.TravelStyleSeeder;
import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static com.example.pinkok_backend.support.GeminiTestSupport.successResponse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 맞춤 추천 테스트. Gemini·카카오맵은 Mockito 로 대체하고, 비동기 처리는 TestAsyncConfig 로 동기 실행한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
class RecommendationTest {

    private static final String TWO_PLACES_JSON = "{\"recommendations\":["
            + "{\"name\":\"성산일출봉\",\"address\":\"제주 서귀포시 성산읍\",\"category\":\"관광지\",\"reason\":\"일출 명소라 커플 여행에 잘 어울려요.\"},"
            + "{\"name\":\"카페 델문도\",\"address\":\"제주 제주시 구좌읍\",\"category\":\"카페\",\"reason\":\"바다가 보이는 분위기 좋은 카페예요.\"}"
            + "]}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    TravelStyleSeeder travelStyleSeeder;

    @MockitoBean
    GeminiClient geminiClient;

    @MockitoBean
    KakaoLocalApiClient kakaoLocalApiClient;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        travelStyleSeeder.seed();
        Mockito.when(geminiClient.modelName()).thenReturn("gemini-2.5-flash-test");
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    // ------------------------------------------------------------------ 도우미

    private String signupAndLogin(String email, String username) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"테스터\"}",
                                email, username)))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"pass1234\"}", email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private long createTrip(String token, String bodyJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long createJejuTrip(String token) throws Exception {
        return createTrip(token, "{\"title\":\"제주 여행\",\"region\":\"제주\",\"companionType\":\"COUPLE\","
                + "\"startDate\":\"2026-10-10\",\"endDate\":\"2026-10-12\"}");
    }

    private long addPin(String token, long tripId, String placeName) throws Exception {
        MvcResult result = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"tripId\":%d,\"place\":{\"name\":\"%s\",\"latitude\":33.39,\"longitude\":126.24}}",
                                tripId, placeName)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private MvcResult requestRecommendation(String token, String bodyJson) throws Exception {
        return mockMvc.perform(post("/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    private String promptSentToGemini() {
        ArgumentCaptor<GeminiGenerateRequest> captor = ArgumentCaptor.forClass(GeminiGenerateRequest.class);
        Mockito.verify(geminiClient).generate(captor.capture());
        return captor.getValue().getContents().get(0).getParts().get(0).getText();
    }

    private static KakaoKeywordSearchResponse kakaoHit(String id, String name) {
        KakaoKeywordSearchResponse.Document doc = new KakaoKeywordSearchResponse.Document();
        doc.setId(id);
        doc.setPlaceName(name);
        doc.setAddressName("제주 서귀포시");
        doc.setRoadAddressName("제주 서귀포시 성산읍 일출로");
        doc.setCategoryGroupCode("AT4");
        doc.setCategoryName("여행 > 관광명소");
        doc.setX("126.942");
        doc.setY("33.458");
        KakaoKeywordSearchResponse response = new KakaoKeywordSearchResponse();
        response.setDocuments(List.of(doc));
        KakaoKeywordSearchResponse.Meta meta = new KakaoKeywordSearchResponse.Meta();
        meta.setEnd(true);
        response.setMeta(meta);
        return response;
    }

    private static KakaoKeywordSearchResponse kakaoNoHit() {
        KakaoKeywordSearchResponse response = new KakaoKeywordSearchResponse();
        response.setDocuments(List.of());
        KakaoKeywordSearchResponse.Meta meta = new KakaoKeywordSearchResponse.Meta();
        meta.setEnd(true);
        response.setMeta(meta);
        return response;
    }

    // ------------------------------------------------------------------ 여행 스타일

    @Test
    @DisplayName("여행 스타일 5종이 시드되고 목록으로 조회된다")
    void travelStyles_areSeeded() throws Exception {
        String token = signupAndLogin("style@pinkok.com", "styleuser");

        mockMvc.perform(get("/travel-styles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[?(@.code=='FOOD')].name").value("맛집 위주"));
    }

    // ------------------------------------------------------------------ 추천 생성

    @Test
    @DisplayName("추천 요청 - 성공하면 추천이 SUGGESTED 로 저장되고, 프롬프트에 지역·동행·스타일·기간이 담긴다")
    void create_success_savesSuggestionsAndBuildsPromptFromTripContext() throws Exception {
        String token = signupAndLogin("rec@pinkok.com", "recuser");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 200, 80));

        MvcResult created = requestRecommendation(token, String.format(
                "{\"tripId\":%d,\"styleCodes\":[\"food\",\"CAFE\"],\"count\":2}", tripId));
        // 동기 실행 환경이라 응답 시점에 이미 끝나 있다 (실제 서버에선 PENDING 으로 오고 폴링한다)
        assertTrue(created.getResponse().getContentAsString().contains("SUCCESS"));

        mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name=='성산일출봉')].status").value("SUGGESTED"))
                .andExpect(jsonPath("$[?(@.name=='성산일출봉')].category").value("관광지"))
                .andExpect(jsonPath("$[?(@.name=='카페 델문도')].reason").value("바다가 보이는 분위기 좋은 카페예요."));

        String prompt = promptSentToGemini();
        assertTrue(prompt.contains("제주"), "여행 지역이 프롬프트에 들어가야 한다");
        assertTrue(prompt.contains("커플"), "여행에 저장된 동행(COUPLE)이 프롬프트에 들어가야 한다");
        assertTrue(prompt.contains("맛집 위주") && prompt.contains("카페"), "요청한 스타일이 프롬프트에 들어가야 한다");
        assertTrue(prompt.contains("2박 3일"), "여행 기간이 프롬프트에 들어가야 한다");
        assertTrue(prompt.contains("최대 2개"));
    }

    @Test
    @DisplayName("요청에 동행을 주면 여행에 저장된 동행보다 우선한다")
    void create_requestCompanionOverridesTrip() throws Exception {
        String token = signupAndLogin("rec2@pinkok.com", "recuser2");
        long tripId = createJejuTrip(token); // 여행에는 COUPLE
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));

        requestRecommendation(token, String.format("{\"tripId\":%d,\"companionType\":\"family\"}", tripId));

        String prompt = promptSentToGemini();
        assertTrue(prompt.contains("가족"));
        assertFalse(prompt.contains("동행: 커플"));
    }

    @Test
    @DisplayName("이미 일정에 있는 장소와 예전에 추천한 장소는 프롬프트에서 제외시키고, 그래도 오면 저장하지 않는다")
    void create_excludesExistingPinsAndPreviousSuggestions() throws Exception {
        String token = signupAndLogin("rec3@pinkok.com", "recuser3");
        long tripId = createJejuTrip(token);
        addPin(token, tripId, "협재 해수욕장");
        String json = "{\"recommendations\":["
                + "{\"name\":\"협재 해수욕장\",\"address\":\"\",\"category\":\"해변\",\"reason\":\"이미 일정에 있음\"},"
                + "{\"name\":\"우도\",\"address\":\"\",\"category\":\"관광지\",\"reason\":\"섬 여행\"},"
                + "{\"name\":\"우 도\",\"address\":\"\",\"category\":\"관광지\",\"reason\":\"응답 안 중복(띄어쓰기만 다름)\"}"
                + "]}";
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(json, 10, 10));

        requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));

        assertTrue(promptSentToGemini().contains("협재 해수욕장"), "일정에 있는 장소는 제외 목록으로 프롬프트에 들어가야 한다");

        mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("우도"));
    }

    @Test
    @DisplayName("count 보다 많이 와도 count 개까지만 저장한다")
    void create_limitsToRequestedCount() throws Exception {
        String token = signupAndLogin("rec4@pinkok.com", "recuser4");
        long tripId = createJejuTrip(token);
        String json = "{\"recommendations\":["
                + "{\"name\":\"A장소\",\"category\":\"관광지\"},{\"name\":\"B장소\",\"category\":\"관광지\"},"
                + "{\"name\":\"C장소\",\"category\":\"관광지\"},{\"name\":\"D장소\",\"category\":\"관광지\"}]}";
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(json, 10, 10));

        requestRecommendation(token, String.format("{\"tripId\":%d,\"count\":3}", tripId));

        mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("모델이 마크다운 코드블록으로 감싸서 답해도 파싱된다")
    void create_parsesCodeFencedJson() throws Exception {
        String token = signupAndLogin("rec5@pinkok.com", "recuser5");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any()))
                .thenReturn(successResponse("```json\n" + TWO_PLACES_JSON + "\n```", 10, 10));

        requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));

        mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("응답이 약속한 JSON 이 아니면 FAILED(PARSE_ERROR) 로 끝나고 추천은 저장되지 않는다")
    void create_malformedResponse_failsWithParseError() throws Exception {
        String token = signupAndLogin("rec6@pinkok.com", "recuser6");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse("죄송하지만 추천할 수 없습니다.", 10, 10));

        MvcResult created = requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));
        Number aiRequestId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/ai-requests/" + aiRequestId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value(org.hamcrest.Matchers.startsWith("PARSE_ERROR")));

        mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("추천 요청도 GET /ai-requests/{id} 로 진행 상태를 볼 수 있고, 장소 추출용 places 는 비어 있다")
    void create_statusIsVisibleThroughAiRequests() throws Exception {
        String token = signupAndLogin("rec7@pinkok.com", "recuser7");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));

        MvcResult created = requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));
        Number aiRequestId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/ai-requests/" + aiRequestId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.places").doesNotExist());
    }

    // ------------------------------------------------------------------ 검증 · 권한

    @Test
    @DisplayName("잘못된 동행 / 모르는 스타일 / 범위 밖 개수는 400, Gemini 는 호출하지 않는다")
    void create_invalidInput_returns400() throws Exception {
        String token = signupAndLogin("rec8@pinkok.com", "recuser8");
        long tripId = createJejuTrip(token);

        for (String body : List.of(
                String.format("{\"tripId\":%d,\"companionType\":\"ALONE\"}", tripId),
                String.format("{\"tripId\":%d,\"styleCodes\":[\"FOOD\",\"NOPE\"]}", tripId),
                String.format("{\"tripId\":%d,\"count\":0}", tripId),
                String.format("{\"tripId\":%d,\"count\":11}", tripId))) {
            mockMvc.perform(post("/recommendations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        Mockito.verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("여행 팀원이 아니면 추천 요청·조회가 403")
    void nonMember_isForbidden() throws Exception {
        String ownerToken = signupAndLogin("owner@pinkok.com", "owneruser");
        long tripId = createJejuTrip(ownerToken);
        String strangerToken = signupAndLogin("stranger@pinkok.com", "strangeruser");

        mockMvc.perform(post("/recommendations")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d}", tripId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("로그인 없이 호출하면 401")
    void withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"tripId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 수락 · 거절

    private long firstSuggestionId(String token, long tripId) throws Exception {
        MvcResult list = mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        // 필터 표현식은 결과가 항상 리스트로 나온다
        List<Number> ids = JsonPath.read(list.getResponse().getContentAsString(), "$[?(@.name=='성산일출봉')].id");
        return ids.get(0).longValue();
    }

    @Test
    @DisplayName("수락 - 카카오맵에서 좌표를 찾아 일정에 핀(addedBy=AI)으로 추가하고 ACCEPTED 로 바뀐다")
    void accept_createsPinFromKakaoPlace() throws Exception {
        String token = signupAndLogin("acc@pinkok.com", "accuser");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));
        requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));
        long recommendationId = firstSuggestionId(token, tripId);

        // 지역을 붙인 첫 질의("제주 성산일출봉")에서 바로 찾는 상황
        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.eq("제주 성산일출봉"), Mockito.eq(1), Mockito.eq(1)))
                .thenReturn(kakaoHit("KAKAO-777", "성산일출봉"));

        mockMvc.perform(post("/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.placeId").isNumber())
                .andExpect(jsonPath("$.itineraryItemId").isNumber());

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].place.name").value("성산일출봉"))
                .andExpect(jsonPath("$[0].addedBy").value("AI"));
    }

    @Test
    @DisplayName("수락 - 지역 포함 질의로 못 찾으면 이름만으로 다시 찾는다")
    void accept_fallsBackToNameOnlyQuery() throws Exception {
        String token = signupAndLogin("acc2@pinkok.com", "accuser2");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));
        requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));
        long recommendationId = firstSuggestionId(token, tripId);

        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.eq("제주 성산일출봉"), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(kakaoNoHit());
        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.eq("성산일출봉"), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(kakaoHit("KAKAO-778", "성산일출봉"));

        mockMvc.perform(post("/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("수락 - 날짜(dayId)를 주면 그 일자 맨 뒤에 들어간다")
    void accept_withDay_putsPinOnThatDay() throws Exception {
        String token = signupAndLogin("acc3@pinkok.com", "accuser3");
        long tripId = createJejuTrip(token);
        MvcResult day = mockMvc.perform(post("/itinerary-days")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":1}", tripId)))
                .andExpect(status().isCreated())
                .andReturn();
        long dayId = ((Number) JsonPath.read(day.getResponse().getContentAsString(), "$.id")).longValue();

        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));
        requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));
        long recommendationId = firstSuggestionId(token, tripId);
        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(kakaoHit("KAKAO-779", "성산일출봉"));

        mockMvc.perform(post("/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"dayId\":%d}", dayId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].dayId").value(dayId))
                .andExpect(jsonPath("$[0].visitOrder").value(1));
    }

    @Test
    @DisplayName("수락 - 지도에서 못 찾으면 422, 추천은 SUGGESTED 로 남고 핀은 생기지 않는다")
    void accept_notFoundOnMap_returns422() throws Exception {
        String token = signupAndLogin("acc4@pinkok.com", "accuser4");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));
        requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));
        long recommendationId = firstSuggestionId(token, tripId);
        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(kakaoNoHit());

        mockMvc.perform(post("/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/recommendations").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[?(@.name=='성산일출봉')].status").value("SUGGESTED"));
        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("이미 수락·거절한 추천은 다시 처리할 수 없다 (409)")
    void alreadyHandled_returns409() throws Exception {
        String token = signupAndLogin("acc5@pinkok.com", "accuser5");
        long tripId = createJejuTrip(token);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));
        requestRecommendation(token, String.format("{\"tripId\":%d}", tripId));
        long recommendationId = firstSuggestionId(token, tripId);

        mockMvc.perform(post("/recommendations/" + recommendationId + "/reject")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/recommendations/" + recommendationId + "/reject")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("팀원이 아니면 수락·거절이 403")
    void acceptAndReject_nonMember_forbidden() throws Exception {
        String ownerToken = signupAndLogin("owner2@pinkok.com", "owneruser2");
        long tripId = createJejuTrip(ownerToken);
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(TWO_PLACES_JSON, 10, 10));
        requestRecommendation(ownerToken, String.format("{\"tripId\":%d}", tripId));
        long recommendationId = firstSuggestionId(ownerToken, tripId);
        String strangerToken = signupAndLogin("stranger2@pinkok.com", "strangeruser2");

        mockMvc.perform(post("/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/recommendations/" + recommendationId + "/reject")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }
}
