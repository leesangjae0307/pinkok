package com.example.pinkok_backend.trip;

import com.example.pinkok_backend.kakao.KakaoAddressSearchResponse;
import com.example.pinkok_backend.kakao.KakaoKeywordSearchResponse;
import com.example.pinkok_backend.kakao.KakaoLocalApiClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 카카오맵 키워드 장소 검색. 실제 카카오 서버는 호출하지 않고 KakaoLocalApiClient 를
 * Mockito 로 대체해서, 우리 코드(응답 매핑·검색 결과를 핀 추가에 바로 쓸 수 있는지)만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlaceSearchTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KakaoLocalApiClient kakaoLocalApiClient;

    private String signupAndLogin() throws Exception {
        String signupBody = "{\"email\":\"searcher@pinkok.com\",\"username\":\"searcher01\",\"password\":\"pass1234\",\"nickname\":\"검색러\"}";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"searcher@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static KakaoKeywordSearchResponse.Document sampleDocument() {
        KakaoKeywordSearchResponse.Document doc = new KakaoKeywordSearchResponse.Document();
        doc.setId("26338954");
        doc.setPlaceName("스타벅스 강남역점");
        doc.setAddressName("서울 강남구 역삼동 858");
        doc.setRoadAddressName("서울 강남구 강남대로 390");
        doc.setCategoryGroupCode("CE7");
        doc.setCategoryName("음식점 > 카페 > 커피전문점 > 스타벅스");
        doc.setPhone("1522-3232");
        doc.setPlaceUrl("http://place.map.kakao.com/26338954");
        doc.setX("127.027618");
        doc.setY("37.497952");
        return doc;
    }

    private static KakaoKeywordSearchResponse responseWith(boolean isEnd, KakaoKeywordSearchResponse.Document... docs) {
        KakaoKeywordSearchResponse response = new KakaoKeywordSearchResponse();
        response.setDocuments(List.of(docs));
        KakaoKeywordSearchResponse.Meta meta = new KakaoKeywordSearchResponse.Meta();
        meta.setTotalCount(docs.length);
        meta.setEnd(isEnd);
        response.setMeta(meta);
        return response;
    }

    @Test
    @DisplayName("검색 결과가 PlaceInput과 같은 모양으로 매핑된다 (x=경도, y=위도 뒤집히지 않음)")
    void search_mapsKakaoResponse() throws Exception {
        String token = signupAndLogin();
        Mockito.when(kakaoLocalApiClient.searchByKeyword("강남역 카페", 1, 15))
                .thenReturn(responseWith(true, sampleDocument()));

        mockMvc.perform(get("/places/search")
                        .param("keyword", "강남역 카페")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].kakaoPlaceId").value("26338954"))
                .andExpect(jsonPath("$.places[0].name").value("스타벅스 강남역점"))
                .andExpect(jsonPath("$.places[0].latitude").value(37.497952))
                .andExpect(jsonPath("$.places[0].longitude").value(127.027618));
    }

    private static KakaoKeywordSearchResponse emptyKeywordResponse() {
        return responseWith(true);
    }

    private static KakaoAddressSearchResponse addressResponse(String building) {
        KakaoAddressSearchResponse.Document doc = new KakaoAddressSearchResponse.Document();
        doc.setAddressName("강원특별자치도 강릉시 임영로207번길 6-1");
        doc.setX("128.8942");
        doc.setY("37.7556");
        KakaoAddressSearchResponse.RoadAddress road = new KakaoAddressSearchResponse.RoadAddress();
        road.setAddressName("강원특별자치도 강릉시 임영로207번길 6-1");
        road.setBuildingName(building);
        doc.setRoadAddress(road);
        KakaoAddressSearchResponse.Address lot = new KakaoAddressSearchResponse.Address();
        lot.setAddressName("강원특별자치도 강릉시 임당동 123-4");
        doc.setAddress(lot);

        KakaoAddressSearchResponse response = new KakaoAddressSearchResponse();
        response.setDocuments(List.of(doc));
        KakaoAddressSearchResponse.Meta meta = new KakaoAddressSearchResponse.Meta();
        meta.setTotalCount(1);
        meta.setEnd(true);
        response.setMeta(meta);
        return response;
    }

    @Test
    @DisplayName("카카오맵 장소 결과가 있으면 type=PLACE 로 오고, 주소 검색은 하지 않는다")
    void search_placeResult_hasTypePlace() throws Exception {
        String token = signupAndLogin();
        Mockito.when(kakaoLocalApiClient.searchByKeyword("강남역 카페", 1, 15))
                .thenReturn(responseWith(true, sampleDocument()));

        mockMvc.perform(get("/places/search")
                        .param("keyword", "강남역 카페")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].type").value("PLACE"));

        Mockito.verify(kakaoLocalApiClient, Mockito.never())
                .searchByAddress(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    @DisplayName("등록된 장소가 없으면 주소로 다시 찾아 type=ADDRESS 로 돌려준다 (이름은 건물명이 없으면 주소)")
    void search_noPlace_fallsBackToAddress() throws Exception {
        String token = signupAndLogin();
        String query = "강릉시 임영로 207번길 6-1";
        Mockito.when(kakaoLocalApiClient.searchByKeyword(query, 1, 15)).thenReturn(emptyKeywordResponse());
        Mockito.when(kakaoLocalApiClient.searchByAddress(query, 1, 15)).thenReturn(addressResponse(""));

        mockMvc.perform(get("/places/search")
                        .param("keyword", query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].type").value("ADDRESS"))
                .andExpect(jsonPath("$.places[0].kakaoPlaceId").doesNotExist())
                .andExpect(jsonPath("$.places[0].name").value("강원특별자치도 강릉시 임영로207번길 6-1"))
                .andExpect(jsonPath("$.places[0].roadAddress").value("강원특별자치도 강릉시 임영로207번길 6-1"))
                .andExpect(jsonPath("$.places[0].lotAddress").value("강원특별자치도 강릉시 임당동 123-4"))
                .andExpect(jsonPath("$.places[0].latitude").value(37.7556))
                .andExpect(jsonPath("$.places[0].longitude").value(128.8942));
    }

    @Test
    @DisplayName("주소 결과에 건물명이 있으면 임시 이름으로 건물명을 쓴다")
    void search_addressWithBuildingName() throws Exception {
        String token = signupAndLogin();
        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(emptyKeywordResponse());
        Mockito.when(kakaoLocalApiClient.searchByAddress(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(addressResponse("임영빌라"));

        mockMvc.perform(get("/places/search")
                        .param("keyword", "임영로207번길 6-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].name").value("임영빌라"));
    }

    @Test
    @DisplayName("주소 검색이 실패해도 검색 자체는 200 + 빈 목록 (원래 장소 검색은 성공했으므로)")
    void search_addressFailure_returnsEmpty() throws Exception {
        String token = signupAndLogin();
        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(emptyKeywordResponse());
        Mockito.when(kakaoLocalApiClient.searchByAddress(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "카카오 주소 검색 실패"));

        mockMvc.perform(get("/places/search")
                        .param("keyword", "없는곳")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0));
    }

    @Test
    @DisplayName("주소 결과에 사용자가 입력한 이름을 넣어 그대로 핀으로 추가할 수 있다 (카카오 ID 없이)")
    void addressResult_withCustomName_isUsableAsPin() throws Exception {
        String token = signupAndLogin();
        MvcResult tripResult = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"강릉 여행\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number tripId = JsonPath.read(tripResult.getResponse().getContentAsString(), "$.id");

        // 앱 흐름: 주소 결과를 고른 뒤 사용자가 "강릉 바다뷰 숙소"라고 이름을 입력해서 추가
        String place = "{\"name\":\"강릉 바다뷰 숙소\","
                + "\"roadAddress\":\"강원특별자치도 강릉시 임영로207번길 6-1\","
                + "\"latitude\":37.7556,\"longitude\":128.8942}";

        mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"place\":%s}", tripId.longValue(), place)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.place.name").value("강릉 바다뷰 숙소"))
                .andExpect(jsonPath("$.place.roadAddress").value("강원특별자치도 강릉시 임영로207번길 6-1"))
                .andExpect(jsonPath("$.place.latitude").value(37.7556));
    }

    @Test
    @DisplayName("검색어 없으면 카카오 호출 없이 400")
    void search_blankKeyword_returns400() throws Exception {
        String token = signupAndLogin();

        mockMvc.perform(get("/places/search")
                        .param("keyword", "")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(kakaoLocalApiClient);
    }

    @Test
    @DisplayName("로그인 없이 검색하면 401")
    void search_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/places/search").param("keyword", "강남역"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("카카오 쪽에서 실패(키 문제 등)하면 그 상태 그대로 응답한다")
    void search_kakaoFailure_propagates() throws Exception {
        String token = signupAndLogin();
        Mockito.when(kakaoLocalApiClient.searchByKeyword(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "카카오 API 키가 설정되지 않았습니다."));

        mockMvc.perform(get("/places/search")
                        .param("keyword", "강남역")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("검색 결과와 같은 모양(kakaoPlaceId/name/latitude/longitude)을 그대로 핀 추가에 쓸 수 있다")
    void searchResult_isUsableAsItineraryItemPlace() throws Exception {
        String token = signupAndLogin();
        Mockito.when(kakaoLocalApiClient.searchByKeyword("강남역 카페", 1, 15))
                .thenReturn(responseWith(true, sampleDocument()));

        // 실제 프론트 흐름: 검색 -> 결과 중 하나를 그대로 place 로 복사해서 핀 추가
        mockMvc.perform(get("/places/search")
                        .param("keyword", "강남역 카페")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String searchedPlace = "{\"kakaoPlaceId\":\"26338954\",\"name\":\"스타벅스 강남역점\","
                + "\"latitude\":37.497952,\"longitude\":127.027618}";

        MvcResult tripResult = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"강남 여행\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number tripId = JsonPath.read(tripResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"place\":%s}", tripId.longValue(), searchedPlace)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.place.name").value("스타벅스 강남역점"))
                .andExpect(jsonPath("$.place.kakaoPlaceId").doesNotExist()); // PlaceResponse엔 kakaoPlaceId를 안 담음(내부 id만 노출)
    }
}
