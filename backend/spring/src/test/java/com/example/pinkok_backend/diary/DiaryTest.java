package com.example.pinkok_backend.diary;

import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TripMember;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.TripMemberRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 장소별 기록: 핀 하나에 사람마다 하나씩, 팀원끼리는 서로 보이고, 수정·삭제는 쓴 사람만.
 *
 * <p>첨부 파일을 실제로 저장했다가 지우는 것까지 확인하므로
 * 업로드 폴더를 테스트용 임시 폴더로 돌리고, @Transactional 없이(= 진짜 커밋되게) 돌린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiaryTest {

    @TempDir
    static Path uploadDir;

    @DynamicPropertySource
    static void uploadDirProperty(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", () -> uploadDir.toString());
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TripRepository tripRepository;

    @Autowired
    TripMemberRepository tripMemberRepository;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
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

    private Long createTrip(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제주도 여행\"}"))
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
        String body = String.format(
                "{\"tripId\":%d,%s\"place\":{\"name\":\"%s\",\"latitude\":33.45,\"longitude\":126.57}}",
                tripId, dayPart, placeName);
        MvcResult result = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    /** 팀원 초대 API는 아직 없으므로(정훈 담당) 테스트에서는 팀원 행을 직접 넣는다. */
    private void joinTrip(Long tripId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Trip trip = tripRepository.findById(tripId).orElseThrow();

        TripMember member = new TripMember();
        member.setId(new TripMember.TripMemberId());
        member.getId().setTripId(tripId);
        member.getId().setUserId(user.getId());
        member.setTrip(trip);
        member.setUser(user);
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        tripMemberRepository.save(member);
    }

    private String uploadPhoto(String token) throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", png);
        return upload(token, new MockMultipartFile("file", "photo.png", "image/png", png.toByteArray()));
    }

    private String uploadVideo(String token) throws Exception {
        byte[] bytes = ("fake-mp4-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8);
        return upload(token, new MockMultipartFile("file", "clip.mp4", "video/mp4", bytes));
    }

    private String upload(String token, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/files")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$[0].url");
    }

    private Path storedFile(String url) {
        return uploadDir.resolve(url.substring("/files/".length()));
    }

    private static String mediaJson(String... fileUrls) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < fileUrls.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"fileUrl\":\"%s\"}", fileUrls[i]));
        }
        return sb.append("]").toString();
    }

    private MvcResult writeDiary(String token, Long pinId, String content, String mediaJson) throws Exception {
        String body = String.format("{\"itineraryItemId\":%d,\"content\":\"%s\",\"rating\":5,\"media\":%s}",
                pinId, content, mediaJson);
        return mockMvc.perform(post("/diaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    // ---------- 테스트 ----------

    @Test
    @DisplayName("기록 작성 - 메모·별점·사진2·영상1이 그대로 저장된다")
    void createDiary_withPhotosAndVideo() throws Exception {
        String token = signupAndLogin("diary1@pinkok.com", "diary1");
        Long tripId = createTrip(token);
        Long pinId = addPin(token, tripId, createDay(token, tripId, 1), "협재해수욕장");

        String photo1 = uploadPhoto(token);
        String photo2 = uploadPhoto(token);
        String video = uploadVideo(token);

        MvcResult result = writeDiary(token, pinId, "물이 맑았다", mediaJson(photo1, photo2, video));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        String json = result.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(json, "$.content")).isEqualTo("물이 맑았다");
        assertThat((Integer) JsonPath.read(json, "$.rating")).isEqualTo(5);
        assertThat((String) JsonPath.read(json, "$.placeName")).isEqualTo("협재해수욕장");
        assertThat((Boolean) JsonPath.read(json, "$.mine")).isTrue();
        assertThat((java.util.List<?>) JsonPath.read(json, "$.media")).hasSize(3);
        assertThat((String) JsonPath.read(json, "$.media[0].mediaType")).isEqualTo("PHOTO");
        // 사진은 업로드할 때 만들어 둔 썸네일이 붙고, 영상 썸네일은 PinLog 작업 때 붙인다
        assertThat((String) JsonPath.read(json, "$.media[0].thumbnailUrl")).endsWith("_thumb.jpg");
        assertThat((String) JsonPath.read(json, "$.media[2].mediaType")).isEqualTo("VIDEO");
        assertThat((String) JsonPath.read(json, "$.media[2].thumbnailUrl")).isNull();
    }

    @Test
    @DisplayName("같은 핀에 내 기록을 또 쓰면 409")
    void createDiary_twiceOnSamePin_returns409() throws Exception {
        String token = signupAndLogin("diary2@pinkok.com", "diary2");
        Long tripId = createTrip(token);
        Long pinId = addPin(token, tripId, createDay(token, tripId, 1), "카멜리아힐");

        assertThat(writeDiary(token, pinId, "좋았다", "[]").getResponse().getStatus()).isEqualTo(201);
        assertThat(writeDiary(token, pinId, "또 씀", "[]").getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("팀원은 같은 핀에 각자 하나씩 쓰고, 서로의 기록이 모두 보인다")
    void teamMembers_writeAndSeeEachOther() throws Exception {
        String ownerToken = signupAndLogin("diary3owner@pinkok.com", "diary3owner");
        String memberToken = signupAndLogin("diary3member@pinkok.com", "diary3member");
        Long tripId = createTrip(ownerToken);
        joinTrip(tripId, "diary3member@pinkok.com");
        Long pinId = addPin(ownerToken, tripId, createDay(ownerToken, tripId, 1), "성산일출봉");

        writeDiary(ownerToken, pinId, "내 기록", "[]");
        writeDiary(memberToken, pinId, "팀원 기록", "[]");

        mockMvc.perform(get("/diaries").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("내 기록"))
                .andExpect(jsonPath("$[0].mine").value(false))
                .andExpect(jsonPath("$[0].author.nickname").value("diary3owner"))
                .andExpect(jsonPath("$[1].content").value("팀원 기록"))
                .andExpect(jsonPath("$[1].mine").value(true));

        // 핀 하나로도 조회할 수 있다
        mockMvc.perform(get("/diaries").param("itineraryItemId", pinId.toString())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("여행 전체 기록은 동선 순서(1일차 1번 → 2번 → 2일차)로 나온다")
    void listByTrip_orderedByRoute() throws Exception {
        String token = signupAndLogin("diary4@pinkok.com", "diary4");
        Long tripId = createTrip(token);
        Long day1 = createDay(token, tripId, 1);
        Long day2 = createDay(token, tripId, 2);

        Long first = addPin(token, tripId, day1, "첫번째");
        Long second = addPin(token, tripId, day1, "두번째");
        Long third = addPin(token, tripId, day2, "셋째날");

        // 일부러 순서를 섞어서 쓴다
        writeDiary(token, third, "C", "[]");
        writeDiary(token, first, "A", "[]");
        writeDiary(token, second, "B", "[]");

        mockMvc.perform(get("/diaries").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("A"))
                .andExpect(jsonPath("$[1].content").value("B"))
                .andExpect(jsonPath("$[2].content").value("C"));
    }

    @Test
    @DisplayName("팀원이 아니면 남의 여행 기록을 쓰지도 보지도 못한다 (403)")
    void nonMember_returns403() throws Exception {
        String ownerToken = signupAndLogin("diary5owner@pinkok.com", "diary5owner");
        String strangerToken = signupAndLogin("diary5other@pinkok.com", "diary5other");
        Long tripId = createTrip(ownerToken);
        Long pinId = addPin(ownerToken, tripId, createDay(ownerToken, tripId, 1), "우도");

        assertThat(writeDiary(strangerToken, pinId, "남의 여행", "[]").getResponse().getStatus()).isEqualTo(403);

        mockMvc.perform(get("/diaries").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("팀원이라도 남의 기록은 수정·삭제할 수 없다 (403)")
    void otherMemberDiary_cannotBeEditedOrDeleted() throws Exception {
        String ownerToken = signupAndLogin("diary6owner@pinkok.com", "diary6owner");
        String memberToken = signupAndLogin("diary6member@pinkok.com", "diary6member");
        Long tripId = createTrip(ownerToken);
        joinTrip(tripId, "diary6member@pinkok.com");
        Long pinId = addPin(ownerToken, tripId, createDay(ownerToken, tripId, 1), "쇠소깍");

        MvcResult mine = writeDiary(ownerToken, pinId, "주인 기록", "[]");
        Long diaryId = ((Number) JsonPath.read(mine.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(patch("/diaries/" + diaryId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"바꿔버리기\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/diaries/" + diaryId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("기록 수정 - 메모를 바꾸고 사진을 갈아끼우면 예전 사진 파일은 지워진다")
    void updateDiary_replacesMediaAndDeletesOldFile() throws Exception {
        String token = signupAndLogin("diary7@pinkok.com", "diary7");
        Long tripId = createTrip(token);
        Long pinId = addPin(token, tripId, createDay(token, tripId, 1), "한라산");

        String oldPhoto = uploadPhoto(token);
        String keptPhoto = uploadPhoto(token);
        String newPhoto = uploadPhoto(token);

        MvcResult created = writeDiary(token, pinId, "처음", mediaJson(oldPhoto, keptPhoto));
        Long diaryId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(patch("/diaries/" + diaryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"고침\",\"rating\":3,\"media\":" + mediaJson(keptPhoto, newPhoto) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("고침"))
                .andExpect(jsonPath("$.rating").value(3))
                .andExpect(jsonPath("$.media.length()").value(2))
                .andExpect(jsonPath("$.media[0].fileUrl").value(keptPhoto));

        assertThat(storedFile(oldPhoto)).doesNotExist();   // 목록에서 빠진 사진
        assertThat(storedFile(keptPhoto)).exists();        // 그대로 남은 사진
        assertThat(storedFile(newPhoto)).exists();
    }

    @Test
    @DisplayName("기록 삭제 - 붙어 있던 파일도 폴더에서 지워진다")
    void deleteDiary_deletesFiles() throws Exception {
        String token = signupAndLogin("diary8@pinkok.com", "diary8");
        Long tripId = createTrip(token);
        Long pinId = addPin(token, tripId, createDay(token, tripId, 1), "함덕");

        String photo = uploadPhoto(token);
        String video = uploadVideo(token);
        MvcResult created = writeDiary(token, pinId, "지울 기록", mediaJson(photo, video));
        Long diaryId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(delete("/diaries/" + diaryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/diaries").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(storedFile(photo)).doesNotExist();
        assertThat(storedFile(video)).doesNotExist();
    }

    @Test
    @DisplayName("첨부 개수 규칙 - 영상 2개나 첨부 11개는 400")
    void mediaLimits_return400() throws Exception {
        String token = signupAndLogin("diary9@pinkok.com", "diary9");
        Long tripId = createTrip(token);
        Long pinId = addPin(token, tripId, createDay(token, tripId, 1), "섭지코지");

        String video1 = uploadVideo(token);
        String video2 = uploadVideo(token);
        assertThat(writeDiary(token, pinId, "영상둘", mediaJson(video1, video2)).getResponse().getStatus())
                .isEqualTo(400);

        String photo = uploadPhoto(token);
        String[] eleven = new String[11];
        java.util.Arrays.fill(eleven, photo);
        MvcResult tooMany = writeDiary(token, pinId, "너무많음", mediaJson(eleven));
        assertThat(tooMany.getResponse().getStatus()).isEqualTo(400);
        assertThat(tooMany.getResponse().getContentAsString()).contains("10개까지");
    }

    @Test
    @DisplayName("잘못된 요청 - 외부 주소/없는 파일/별점 6/조건 없는 조회는 400, 토큰 없으면 401")
    void invalidRequests() throws Exception {
        String token = signupAndLogin("diary10@pinkok.com", "diary10");
        Long tripId = createTrip(token);
        Long pinId = addPin(token, tripId, createDay(token, tripId, 1), "비자림");

        assertThat(writeDiary(token, pinId, "외부주소", mediaJson("https://example.com/cat.jpg"))
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(writeDiary(token, pinId, "없는파일", mediaJson("/files/2026/09/none.jpg"))
                .getResponse().getStatus()).isEqualTo(400);

        mockMvc.perform(post("/diaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"itineraryItemId\":%d,\"rating\":6}", pinId)))
                .andExpect(status().isBadRequest());

        // tripId / itineraryItemId 둘 다 없거나 둘 다 있으면 400
        mockMvc.perform(get("/diaries").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/diaries")
                        .param("tripId", tripId.toString())
                        .param("itineraryItemId", pinId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/diaries").param("tripId", tripId.toString()))
                .andExpect(status().isUnauthorized());
    }
}
