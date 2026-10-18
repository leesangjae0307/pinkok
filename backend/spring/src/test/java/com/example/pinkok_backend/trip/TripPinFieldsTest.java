package com.example.pinkok_backend.trip;

import com.example.pinkok_backend.entity.Diary;
import com.example.pinkok_backend.entity.DiaryMedia;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.repository.DiaryMediaRepository;
import com.example.pinkok_backend.repository.DiaryRepository;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 프론트가 요청한 필드: 여행의 placesCount, 핀의 rating · reviewsCount · thumbnailUrl. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripPinFieldsTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TripRepository tripRepository;
    @Autowired
    TripMemberRepository tripMemberRepository;
    @Autowired
    ItineraryItemRepository itineraryItemRepository;
    @Autowired
    DiaryRepository diaryRepository;
    @Autowired
    DiaryMediaRepository diaryMediaRepository;

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

    /** 배열에서 id 가 맞는 항목의 필드 하나 (JsonPath 필터는 결과를 리스트로 돌려준다) */
    private static Object field(String body, long id, String field) {
        java.util.List<Object> found = JsonPath.read(body, "$[?(@.id==" + id + ")]." + field);
        return found.isEmpty() ? null : found.get(0);
    }

    private long idOf(MvcResult r) throws Exception {
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long createTrip(String token, String title) throws Exception {
        return idOf(mockMvc.perform(post("/trips").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated()).andReturn());
    }

    private long addPin(String token, long tripId, String name) throws Exception {
        return idOf(mockMvc.perform(post("/itinerary-items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"place\":{\"name\":\"%s\",\"latitude\":33.4,\"longitude\":126.5}}", tripId, name)))
                .andExpect(status().isCreated()).andReturn());
    }

    private Diary addDiary(long itemId, String email, Integer rating) {
        ItineraryItem item = itineraryItemRepository.findById(itemId).orElseThrow();
        // 팀원이 아니어도 데이터로는 넣을 수 있게, 작성자가 이 여행 참여자인지는 여기서 보장하지 않는다(통계만 본다)
        Diary diary = new Diary();
        diary.setItineraryItem(item);
        diary.setUser(userRepository.findByEmail(email).orElseThrow());
        diary.setContent("메모");
        diary.setRating(rating);
        diary.setCreatedAt(LocalDateTime.now());
        diary.setUpdatedAt(LocalDateTime.now());
        return diaryRepository.saveAndFlush(diary);
    }

    private void addMedia(Diary diary, String type, String fileUrl, String thumbnailUrl, int order) {
        DiaryMedia media = new DiaryMedia();
        media.setDiary(diary);
        media.setMediaType(type);
        media.setFileUrl(fileUrl);
        media.setThumbnailUrl(thumbnailUrl);
        media.setDisplayOrder(order);
        media.setCreatedAt(LocalDateTime.now());
        diaryMediaRepository.saveAndFlush(media);
    }

    private void addMember(long tripId, String email) {
        TripMember member = new TripMember();
        TripMember.TripMemberId id = new TripMember.TripMemberId();
        id.setTripId(tripId);
        id.setUserId(userRepository.findByEmail(email).orElseThrow().getId());
        member.setId(id);
        member.setTrip(tripRepository.findById(tripId).orElseThrow());
        member.setUser(userRepository.findByEmail(email).orElseThrow());
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        tripMemberRepository.saveAndFlush(member);
    }

    // ---------------- placesCount ----------------

    @Test
    @DisplayName("placesCount - 새 여행은 0, 핀을 추가·삭제하면 목록과 상세 모두에서 따라간다")
    void placesCount_followsPins() throws Exception {
        String token = signupAndLogin("pf1@pinkok.com", "pfuser1");
        long tripId = createTrip(token, "제주");

        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.placesCount").value(0));

        long a = addPin(token, tripId, "A");
        addPin(token, tripId, "B");
        addPin(token, tripId, "C");
        mockMvc.perform(get("/trips/" + tripId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.placesCount").value(3));
        mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].placesCount").value(3));

        mockMvc.perform(delete("/itinerary-items/" + a).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].placesCount").value(2));
    }

    @Test
    @DisplayName("placesCount - 여행마다 따로 센다 (목록에서 서로 섞이지 않는다), 수정 응답에도 들어 있다")
    void placesCount_isPerTrip() throws Exception {
        String token = signupAndLogin("pf2@pinkok.com", "pfuser2");
        long empty = createTrip(token, "빈 여행");
        long two = createTrip(token, "두 곳");
        long one = createTrip(token, "한 곳");
        addPin(token, two, "A");
        addPin(token, two, "B");
        addPin(token, one, "C");

        MvcResult list = mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        String body = list.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(0, ((Number) field(body, empty, "placesCount")).intValue());
        org.junit.jupiter.api.Assertions.assertEquals(2, ((Number) field(body, two, "placesCount")).intValue());
        org.junit.jupiter.api.Assertions.assertEquals(1, ((Number) field(body, one, "placesCount")).intValue());

        mockMvc.perform(patch("/trips/" + two).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"이름 바꿈\"}"))
                .andExpect(jsonPath("$.placesCount").value(2));
        mockMvc.perform(post("/trips").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"새 여행\"}"))
                .andExpect(jsonPath("$.placesCount").value(0));
    }

    @Test
    @DisplayName("placesCount - 여행이 하나도 없어도 목록은 빈 배열")
    void placesCount_emptyList() throws Exception {
        String token = signupAndLogin("pf3@pinkok.com", "pfuser3");

        mockMvc.perform(get("/trips").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------- rating / reviewsCount ----------------

    @Test
    @DisplayName("기록이 없는 핀은 rating=null, reviewsCount=0, thumbnailUrl=null")
    void noDiaries_defaults() throws Exception {
        String token = signupAndLogin("pf4@pinkok.com", "pfuser4");
        long tripId = createTrip(token, "제주");
        addPin(token, tripId, "A");

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].rating").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[0].reviewsCount").value(0))
                .andExpect(jsonPath("$[0].thumbnailUrl").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("팀원들의 별점 평균(소수 첫째 자리)과 기록 수, 별점을 안 매긴 기록은 평균에서 빠지고 개수에는 들어간다")
    void ratingAverage_andCount() throws Exception {
        String owner = signupAndLogin("pf5@pinkok.com", "pfuser5");
        signupAndLogin("pf5b@pinkok.com", "pfuser5b");
        signupAndLogin("pf5c@pinkok.com", "pfuser5c");
        long tripId = createTrip(owner, "제주");
        addMember(tripId, "pf5b@pinkok.com");
        addMember(tripId, "pf5c@pinkok.com");
        long pin = addPin(owner, tripId, "A");
        long other = addPin(owner, tripId, "B");

        addDiary(pin, "pf5@pinkok.com", 5);
        addDiary(pin, "pf5b@pinkok.com", 4);
        addDiary(pin, "pf5c@pinkok.com", null);   // 별점 없는 기록

        MvcResult result = mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(4.5, ((Number) field(body, pin, "rating")).doubleValue());
        org.junit.jupiter.api.Assertions.assertEquals(3, ((Number) field(body, pin, "reviewsCount")).intValue());
        // 기록이 없는 다른 핀은 영향 없음
        org.junit.jupiter.api.Assertions.assertEquals(0, ((Number) field(body, other, "reviewsCount")).intValue());
    }

    @Test
    @DisplayName("평균은 소수 첫째 자리로 반올림 (5, 4, 4 → 4.3)")
    void ratingIsRoundedToOneDecimal() throws Exception {
        String owner = signupAndLogin("pf6@pinkok.com", "pfuser6");
        signupAndLogin("pf6b@pinkok.com", "pfuser6b");
        signupAndLogin("pf6c@pinkok.com", "pfuser6c");
        long tripId = createTrip(owner, "제주");
        long pin = addPin(owner, tripId, "A");
        addDiary(pin, "pf6@pinkok.com", 5);
        addDiary(pin, "pf6b@pinkok.com", 4);
        addDiary(pin, "pf6c@pinkok.com", 4);

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$[0].rating").value(4.3));
    }

    // ---------------- thumbnailUrl ----------------

    @Test
    @DisplayName("썸네일 - 첫 번째로 올라온 미디어의 썸네일, 사진은 썸네일이 없으면 원본, 나중에 올린 기록이 덮어쓰지 않는다")
    void thumbnail_isFirstMedia() throws Exception {
        String owner = signupAndLogin("pf7@pinkok.com", "pfuser7");
        signupAndLogin("pf7b@pinkok.com", "pfuser7b");
        long tripId = createTrip(owner, "제주");
        addMember(tripId, "pf7b@pinkok.com");
        long withThumb = addPin(owner, tripId, "썸네일 있음");
        long imageOnly = addPin(owner, tripId, "원본만");
        long videoNoThumb = addPin(owner, tripId, "영상만");

        Diary first = addDiary(withThumb, "pf7@pinkok.com", 5);
        addMedia(first, "IMAGE", "/files/a.jpg", "/files/a-thumb.jpg", 1);
        addMedia(first, "IMAGE", "/files/a2.jpg", "/files/a2-thumb.jpg", 2);
        Diary later = addDiary(withThumb, "pf7b@pinkok.com", 4);
        addMedia(later, "IMAGE", "/files/later.jpg", "/files/later-thumb.jpg", 1);

        Diary d2 = addDiary(imageOnly, "pf7@pinkok.com", 3);
        addMedia(d2, "IMAGE", "/files/b.jpg", null, 1);

        Diary d3 = addDiary(videoNoThumb, "pf7@pinkok.com", 3);
        addMedia(d3, "VIDEO", "/files/c.mp4", null, 1);

        String body = mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals("/files/a-thumb.jpg",
                field(body, withThumb, "thumbnailUrl"));
        org.junit.jupiter.api.Assertions.assertEquals("/files/b.jpg",
                field(body, imageOnly, "thumbnailUrl"));
        // 영상인데 썸네일이 없으면 보여줄 게 없다 → null 이지만 기록 수는 센다
        org.junit.jupiter.api.Assertions.assertNull(field(body, videoNoThumb, "thumbnailUrl"));
        org.junit.jupiter.api.Assertions.assertEquals(1, ((Number) field(body, videoNoThumb, "reviewsCount")).intValue());
        org.junit.jupiter.api.Assertions.assertEquals(2, ((Number) field(body, withThumb, "reviewsCount")).intValue());
    }

    @Test
    @DisplayName("미디어 없이 별점만 있는 기록은 썸네일 null, 다른 여행의 기록은 섞이지 않는다")
    void noMedia_andIsolatedPerTrip() throws Exception {
        String owner = signupAndLogin("pf8@pinkok.com", "pfuser8");
        long tripA = createTrip(owner, "A");
        long tripB = createTrip(owner, "B");
        long pinA = addPin(owner, tripA, "A핀");
        addPin(owner, tripB, "B핀");
        addDiary(pinA, "pf8@pinkok.com", 5);

        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripA))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$[0].rating").value(5.0))
                .andExpect(jsonPath("$[0].reviewsCount").value(1))
                .andExpect(jsonPath("$[0].thumbnailUrl").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(get("/itinerary-items").param("tripId", String.valueOf(tripB))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$[0].reviewsCount").value(0));
    }

    @Test
    @DisplayName("핀 수정·순서 변경 응답에도 같은 통계가 들어 있고, 새로 만든 핀은 기록 없음")
    void updateAndReorderResponsesIncludeStats() throws Exception {
        String owner = signupAndLogin("pf9@pinkok.com", "pfuser9");
        long tripId = createTrip(owner, "제주");
        long day = idOf(mockMvc.perform(post("/itinerary-days").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayNumber\":1}", tripId)))
                .andExpect(status().isCreated()).andReturn());
        long pin = idOf(mockMvc.perform(post("/itinerary-items").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"tripId\":%d,\"dayId\":%d,\"place\":{\"name\":\"A\",\"latitude\":33.4,\"longitude\":126.5}}", tripId, day)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewsCount").value(0))     // 방금 만든 핀
                .andReturn());
        Diary diary = addDiary(pin, "pf9@pinkok.com", 4);
        addMedia(diary, "IMAGE", "/files/x.jpg", "/files/x-thumb.jpg", 1);

        mockMvc.perform(patch("/itinerary-items/" + pin).header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":\"수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4.0))
                .andExpect(jsonPath("$.reviewsCount").value(1))
                .andExpect(jsonPath("$.thumbnailUrl").value("/files/x-thumb.jpg"));

        mockMvc.perform(patch("/itinerary-items/order").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"dayId\":%d,\"itemIds\":[%d]}", day, pin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewsCount").value(1))
                .andExpect(jsonPath("$[0].thumbnailUrl").value("/files/x-thumb.jpg"));
    }
}
