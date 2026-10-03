package com.example.pinkok_backend.ai;

import com.example.pinkok_backend.gemini.GeminiClient;
import com.example.pinkok_backend.gemini.GeminiGenerateResponse;
import com.example.pinkok_backend.kakao.KakaoKeywordSearchResponse;
import com.example.pinkok_backend.kakao.KakaoLocalApiClient;
import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 장소 후보 (지은 담당 — 좌표 변환 · 후보 저장 · 핀 추가).
 *
 * <p>Gemini와 카카오맵은 둘 다 Mockito로 대체한다. 우리 코드(좌표 변환 규칙, 신뢰도 계산,
 * 중복 방지, 권한)만 검증하는 것이 목적이고 실제 외부 서버를 부르면 비용도 들고 결과도 그때그때 달라지기 때문이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
class PlaceCandidateTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @MockitoBean
    GeminiClient geminiClient;

    @MockitoBean
    KakaoLocalApiClient kakaoLocalApiClient;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        Mockito.when(geminiClient.modelName()).thenReturn("gemini-2.5-flash-test");
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    // ---------- 준비용 도우미 ----------

    private String signupAndLogin(String email, String username) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"%s\"}",
                                email, username, username)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"pass1234\"}", email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    /** Gemini가 이런 장소들을 뽑았다고 치고 응답을 고정한다. */
    private void geminiExtracts(String placesJson) {
        GeminiGenerateResponse.Part part = new GeminiGenerateResponse.Part();
        part.setText("{\"title\":\"제주 여행\",\"places\":[" + placesJson + "]}");
        GeminiGenerateResponse.Content content = new GeminiGenerateResponse.Content();
        content.setParts(List.of(part));
        GeminiGenerateResponse.Candidate candidate = new GeminiGenerateResponse.Candidate();
        candidate.setContent(content);
        candidate.setFinishReason("STOP");

        GeminiGenerateResponse.UsageMetadata usage = new GeminiGenerateResponse.UsageMetadata();
        usage.setPromptTokenCount(100);
        usage.setCandidatesTokenCount(30);

        GeminiGenerateResponse response = new GeminiGenerateResponse();
        response.setCandidates(List.of(candidate));
        response.setUsageMetadata(usage);

        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(response);
    }

    private static String place(String name, String address) {
        return String.format(
                "{\"name\":\"%s\",\"address\":\"%s\",\"category\":\"해변\",\"description\":\"설명\",\"lat\":33.39,\"lng\":126.24}",
                name, address);
    }

    /** 카카오맵이 이런 장소를 1등으로 돌려준다고 치고 응답을 고정한다. */
    private void kakaoFinds(String kakaoPlaceId, String placeName, String longitudeX, String latitudeY) {
        KakaoKeywordSearchResponse.Document document = new KakaoKeywordSearchResponse.Document();
        document.setId(kakaoPlaceId);
        document.setPlaceName(placeName);
        document.setAddressName("제주특별자치도 제주시 한림읍 협재리");
        document.setRoadAddressName("제주특별자치도 제주시 한림읍 협재해변로");
        document.setCategoryGroupCode("AT4");
        document.setCategoryName("여행 > 관광명소");
        document.setPhone("064-000-0000");
        document.setPlaceUrl("http://place.map.kakao.com/" + kakaoPlaceId);
        document.setX(longitudeX);
        document.setY(latitudeY);

        KakaoKeywordSearchResponse response = new KakaoKeywordSearchResponse();
        response.setDocuments(List.of(document));
        Mockito.doReturn(response).when(kakaoLocalApiClient)
                .searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

    /** 카카오맵에서 아무것도 못 찾은 경우. */
    private void kakaoFindsNothing() {
        KakaoKeywordSearchResponse response = new KakaoKeywordSearchResponse();
        response.setDocuments(List.of());
        // when(...) 대신 doReturn(...) 을 쓰는 이유: 이미 "예외를 던지도록" 정해둔 mock 에 when(...) 을 쓰면
        // 괄호 안의 호출이 실제로 실행되면서 그 예외가 그대로 터진다 (테스트 도중 응답을 바꿔 끼울 때 문제가 된다).
        Mockito.doReturn(response).when(kakaoLocalApiClient)
                .searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

    /** 카카오 서버 장애. */
    private void kakaoIsDown() {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 장소 검색 호출에 실패했습니다."))
                .when(kakaoLocalApiClient).searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

    private Long requestExtraction(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"TEXT\",\"sourceText\":\"협재 해수욕장 다녀왔어요\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private Long createTrip(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제주도 여행\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String candidates(String token, Long aiRequestId) throws Exception {
        return mockMvc.perform(get("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private MvcResult addToTrip(String token, Long tripId, Long... candidateIds) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < candidateIds.length; i++) {
            if (i > 0) ids.append(",");
            ids.append(candidateIds[i]);
        }
        return mockMvc.perform(post("/place-candidates/add-to-trip")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"candidateIds\":[%s]}", tripId, ids)))
                .andReturn();
    }

    // ---------- 테스트 ----------

    @Test
    @DisplayName("AI 추출이 성공하면 후보가 자동으로 만들어지고 카카오맵 좌표가 붙는다")
    void candidatesAreCreatedAutomatically() throws Exception {
        String token = signupAndLogin("cand1@pinkok.com", "cand1");
        geminiExtracts(place("협재해수욕장", "제주시 한림읍"));
        kakaoFinds("26338954", "협재해수욕장", "126.239621", "33.394341");

        Long aiRequestId = requestExtraction(token);

        mockMvc.perform(get("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rawName").value("협재해수욕장"))
                .andExpect(jsonPath("$[0].mapped").value(true))
                .andExpect(jsonPath("$[0].selected").value(false))
                // 카카오는 x=경도, y=위도로 준다. 뒤집어 넣으면 지구 반대편에 핀이 꽂힌다.
                .andExpect(jsonPath("$[0].place.latitude").value(33.394341))
                .andExpect(jsonPath("$[0].place.longitude").value(126.239621))
                .andExpect(jsonPath("$[0].place.name").value("협재해수욕장"));
    }

    @Test
    @DisplayName("카카오맵에서 못 찾은 장소도 목록에는 남고(mapped=false), 신뢰도는 0.3")
    void notFoundPlace_staysInListButUnmapped() throws Exception {
        String token = signupAndLogin("cand2@pinkok.com", "cand2");
        geminiExtracts(place("그 바닷가 근처 카페", ""));
        kakaoFindsNothing();

        Long aiRequestId = requestExtraction(token);

        mockMvc.perform(get("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mapped").value(false))
                .andExpect(jsonPath("$[0].place").doesNotExist())
                .andExpect(jsonPath("$[0].confidence").value(0.300));
    }

    @Test
    @DisplayName("신뢰도 - 이름이 똑같으면 1.0, 한쪽이 다른 쪽을 포함하면 0.8, 그 외는 0.6")
    void confidenceReflectsNameMatch() throws Exception {
        String exactToken = signupAndLogin("cand3a@pinkok.com", "cand3a");
        geminiExtracts(place("협재 해수욕장", "제주시"));
        kakaoFinds("1", "협재해수욕장", "126.23", "33.39");   // 띄어쓰기만 다름 = 같은 이름
        assertThat((Double) JsonPath.read(candidates(exactToken, requestExtraction(exactToken)), "$[0].confidence"))
                .isEqualTo(1.0);

        String partialToken = signupAndLogin("cand3b@pinkok.com", "cand3b");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoFinds("2", "협재해수욕장 공영주차장", "126.23", "33.39");
        assertThat((Double) JsonPath.read(candidates(partialToken, requestExtraction(partialToken)), "$[0].confidence"))
                .isEqualTo(0.8);

        String looseToken = signupAndLogin("cand3c@pinkok.com", "cand3c");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoFinds("3", "한림읍 행정복지센터", "126.23", "33.39");
        assertThat((Double) JsonPath.read(candidates(looseToken, requestExtraction(looseToken)), "$[0].confidence"))
                .isEqualTo(0.6);
    }

    @Test
    @DisplayName("남의 AI 요청 후보는 볼 수 없다 (403)")
    void otherUsersCandidates_returns403() throws Exception {
        String owner = signupAndLogin("cand4owner@pinkok.com", "cand4owner");
        String stranger = signupAndLogin("cand4other@pinkok.com", "cand4other");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoFinds("26338954", "협재해수욕장", "126.23", "33.39");

        Long aiRequestId = requestExtraction(owner);

        mockMvc.perform(get("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("후보 재생성을 눌러도 이미 있으면 중복으로 만들어지지 않는다")
    void recreate_doesNotDuplicate() throws Exception {
        String token = signupAndLogin("cand5@pinkok.com", "cand5");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoFinds("26338954", "협재해수욕장", "126.23", "33.39");

        Long aiRequestId = requestExtraction(token);
        Integer firstId = JsonPath.read(candidates(token, aiRequestId), "$[0].id");

        mockMvc.perform(post("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstId));
    }

    @Test
    @DisplayName("체크한 후보를 여행에 추가하면 날짜 미배정 핀으로 꽂힌다")
    void addToTrip_createsUnassignedPin() throws Exception {
        String token = signupAndLogin("cand6@pinkok.com", "cand6");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoFinds("26338954", "협재해수욕장", "126.239621", "33.394341");

        Long aiRequestId = requestExtraction(token);
        Long tripId = createTrip(token);
        Integer candidateId = JsonPath.read(candidates(token, aiRequestId), "$[0].id");

        MvcResult result = addToTrip(token, tripId, candidateId.longValue());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String json = result.getResponse().getContentAsString();
        assertThat((List<?>) JsonPath.read(json, "$.added")).hasSize(1);
        assertThat((String) JsonPath.read(json, "$.added[0].place.name")).isEqualTo("협재해수욕장");
        assertThat((String) JsonPath.read(json, "$.added[0].addedBy")).isEqualTo("AI");
        assertThat((Object) JsonPath.read(json, "$.added[0].dayId")).isNull();

        // 실제 여행 일정에서도 보여야 한다
        mockMvc.perform(get("/itinerary-items").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].place.name").value("협재해수욕장"));

        // 추가한 후보는 체크 상태로 남는다
        mockMvc.perform(get("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].selected").value(true));
    }

    @Test
    @DisplayName("이미 그 여행에 있는 장소는 건너뛴다 (skippedDuplicate)")
    void addToTrip_duplicatePlace_isSkipped() throws Exception {
        String token = signupAndLogin("cand7@pinkok.com", "cand7");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoFinds("26338954", "협재해수욕장", "126.23", "33.39");

        Long aiRequestId = requestExtraction(token);
        Long tripId = createTrip(token);
        Integer candidateId = JsonPath.read(candidates(token, aiRequestId), "$[0].id");

        addToTrip(token, tripId, candidateId.longValue());
        String json = addToTrip(token, tripId, candidateId.longValue()).getResponse().getContentAsString();

        assertThat((List<?>) JsonPath.read(json, "$.added")).isEmpty();
        assertThat((List<?>) JsonPath.read(json, "$.skippedDuplicate")).hasSize(1);

        mockMvc.perform(get("/itinerary-items").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("좌표를 못 찾은 후보는 핀으로 추가할 수 없다 (skippedNoCoordinate)")
    void addToTrip_unmappedCandidate_isSkipped() throws Exception {
        String token = signupAndLogin("cand8@pinkok.com", "cand8");
        geminiExtracts(place("어딘가의 그 카페", ""));
        kakaoFindsNothing();

        Long aiRequestId = requestExtraction(token);
        Long tripId = createTrip(token);
        Integer candidateId = JsonPath.read(candidates(token, aiRequestId), "$[0].id");

        String json = addToTrip(token, tripId, candidateId.longValue()).getResponse().getContentAsString();

        assertThat((List<?>) JsonPath.read(json, "$.added")).isEmpty();
        assertThat((List<?>) JsonPath.read(json, "$.skippedNoCoordinate")).hasSize(1);
    }

    @Test
    @DisplayName("팀원이 아닌 여행에는 후보를 추가할 수 없다 (403)")
    void addToTrip_notAMember_returns403() throws Exception {
        String owner = signupAndLogin("cand9owner@pinkok.com", "cand9owner");
        String stranger = signupAndLogin("cand9other@pinkok.com", "cand9other");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoFinds("26338954", "협재해수욕장", "126.23", "33.39");

        Long strangerRequestId = requestExtraction(stranger);
        Long ownerTripId = createTrip(owner);
        Integer candidateId = JsonPath.read(candidates(stranger, strangerRequestId), "$[0].id");

        assertThat(addToTrip(stranger, ownerTripId, candidateId.longValue()).getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    @DisplayName("카카오맵이 죽어도 AI 결과는 살아남고, 나중에 재생성으로 후보를 만들 수 있다")
    void kakaoDown_keepsAiResultAndAllowsRetry() throws Exception {
        String token = signupAndLogin("cand10@pinkok.com", "cand10");
        geminiExtracts(place("협재해수욕장", "제주시"));
        kakaoIsDown();

        Long aiRequestId = requestExtraction(token);   // AI 요청 자체는 SUCCESS 로 끝나야 한다

        // 반쪽짜리 목록을 남기지 않는다 ("검색 결과 없음"과 "카카오 장애"는 다르다)
        mockMvc.perform(get("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));

        // 카카오가 살아나면 재생성으로 복구된다
        kakaoFinds("26338954", "협재해수욕장", "126.23", "33.39");
        mockMvc.perform(post("/place-candidates").param("aiRequestId", aiRequestId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mapped").value(true));
    }

    @Test
    @DisplayName("토큰 없이 부르면 401")
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/place-candidates").param("aiRequestId", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/place-candidates/add-to-trip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":1,\"candidateIds\":[1]}"))
                .andExpect(status().isUnauthorized());
    }
}
