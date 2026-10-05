package com.example.pinkok_backend.pinlog;

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
import org.springframework.context.annotation.Import;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PinLog: 핀마다 클립을 골라 미니 브이로그로 합치기.
 *
 * <p>FFmpeg 대신 가짜 합성기를 쓴다({@link TestVideoComposerConfig}). 합성은 별도 스레드에서
 * 돌기 때문에, 요청 직후에는 QUEUED 이고 조금 뒤에 DONE 이 된다 — 그래서 상태가 바뀔 때까지 기다렸다 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestVideoComposerConfig.class)
class PinlogTest {

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
    TestVideoComposerConfig.FakeVideoComposer fakeVideoComposer;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TripRepository tripRepository;

    @Autowired
    TripMemberRepository tripMemberRepository;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        fakeVideoComposer.reset();
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
        MvcResult result = mockMvc.perform(post("/itinerary-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"tripId\":%d,\"dayId\":%d,\"place\":{\"name\":\"%s\",\"latitude\":33.45,\"longitude\":126.57}}",
                                tripId, dayId, placeName)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String uploadVideo(String token) throws Exception {
        byte[] bytes = ("fake-mp4-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8);
        return upload(token, new MockMultipartFile("file", "clip.mp4", "video/mp4", bytes));
    }

    private String uploadPhoto(String token) throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", png);
        return upload(token, new MockMultipartFile("file", "photo.png", "image/png", png.toByteArray()));
    }

    private String upload(String token, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/files")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$[0].url");
    }

    /** 핀에 기록을 남기고, 거기 붙은 미디어 ID를 돌려준다. */
    private Long writeDiaryWithMedia(String token, Long pinId, String fileUrl) throws Exception {
        MvcResult result = mockMvc.perform(post("/diaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"itineraryItemId\":%d,\"content\":\"메모\",\"media\":[{\"fileUrl\":\"%s\"}]}",
                                pinId, fileUrl)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.media[0].id")).longValue();
    }

    private MvcResult createPinlog(String token, String body) throws Exception {
        return mockMvc.perform(post("/pinlogs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    /** 합성은 다른 스레드에서 돌아서 바로 끝나지 않는다. 상태가 바뀔 때까지 잠깐 기다린다. */
    private String awaitFinalStatus(String token, Long pinlogId) throws Exception {
        for (int i = 0; i < 100; i++) {
            String json = mockMvc.perform(get("/pinlogs/" + pinlogId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String status = JsonPath.read(json, "$.status");
            if ("DONE".equals(status) || "FAILED".equals(status)) {
                return json;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("합성이 5초 안에 끝나지 않았습니다.");
    }

    private Path storedFile(String url) {
        return uploadDir.resolve(url.substring("/files/".length()));
    }

    // ---------- 테스트 ----------

    @Test
    @DisplayName("고른 클립으로 PinLog 를 만들면 QUEUED 로 응답하고, 합성이 끝나면 DONE + videoUrl")
    void createPinlog_withChosenClips() throws Exception {
        String token = signupAndLogin("pinlog1@pinkok.com", "pinlog1");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        Long media1 = writeDiaryWithMedia(token, addPin(token, tripId, dayId, "협재해수욕장"), uploadVideo(token));
        Long media2 = writeDiaryWithMedia(token, addPin(token, tripId, dayId, "카멜리아힐"), uploadVideo(token));

        MvcResult created = createPinlog(token, String.format(
                "{\"tripId\":%d,\"title\":\"제주 3박4일\",\"clips\":[{\"diaryMediaId\":%d},{\"diaryMediaId\":%d}]}",
                tripId, media2, media1));

        assertThat(created.getResponse().getStatus()).isEqualTo(202);
        String createdJson = created.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(createdJson, "$.status")).isEqualTo("QUEUED");
        // 보낸 순서가 그대로 클립 순서가 된다 (media2 가 1번)
        assertThat((Integer) JsonPath.read(createdJson, "$.clips[0].diaryMediaId")).isEqualTo(media2.intValue());
        assertThat((Integer) JsonPath.read(createdJson, "$.clips[0].clipOrder")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(createdJson, "$.clips[1].diaryMediaId")).isEqualTo(media1.intValue());

        Long pinlogId = ((Number) JsonPath.read(createdJson, "$.id")).longValue();
        String doneJson = awaitFinalStatus(token, pinlogId);

        assertThat((String) JsonPath.read(doneJson, "$.status")).isEqualTo("DONE");
        assertThat((String) JsonPath.read(doneJson, "$.videoUrl")).endsWith(".mp4");
        assertThat((String) JsonPath.read(doneJson, "$.thumbnailUrl")).endsWith("_thumb.jpg");
        assertThat((String) JsonPath.read(doneJson, "$.resolution")).isEqualTo("1280x720");
        assertThat((Integer) JsonPath.read(doneJson, "$.durationMs")).isPositive();
        assertThat(storedFile(JsonPath.read(doneJson, "$.videoUrl"))).exists();
    }

    @Test
    @DisplayName("클립을 안 보내면 동선 순서로 자동 선택된다 (핀마다 내 영상 우선)")
    void createPinlog_autoSelectsClips() throws Exception {
        String owner = signupAndLogin("pinlog2@pinkok.com", "pinlog2");
        String mate = signupAndLogin("pinlog2mate@pinkok.com", "pinlog2mate");
        Long tripId = createTrip(owner);
        joinTrip(tripId, "pinlog2mate@pinkok.com");
        Long dayId = createDay(owner, tripId, 1);

        Long pin1 = addPin(owner, tripId, dayId, "협재해수욕장");
        Long pin2 = addPin(owner, tripId, dayId, "카멜리아힐");

        // 1번 핀: 팀원도 나도 찍었다 -> 내 영상이 뽑혀야 한다
        writeDiaryWithMedia(mate, pin1, uploadVideo(mate));
        Long myVideo = writeDiaryWithMedia(owner, pin1, uploadVideo(owner));
        // 2번 핀: 나는 깜빡했고 팀원만 찍었다 -> 팀원 영상으로 채워져야 한다
        Long mateVideo = writeDiaryWithMedia(mate, pin2, uploadVideo(mate));

        MvcResult created = createPinlog(owner, String.format("{\"tripId\":%d}", tripId));
        assertThat(created.getResponse().getStatus()).isEqualTo(202);

        String json = created.getResponse().getContentAsString();
        assertThat((List<?>) JsonPath.read(json, "$.clips")).hasSize(2);
        assertThat((Integer) JsonPath.read(json, "$.clips[0].diaryMediaId")).isEqualTo(myVideo.intValue());
        assertThat((Integer) JsonPath.read(json, "$.clips[1].diaryMediaId")).isEqualTo(mateVideo.intValue());
    }

    @Test
    @DisplayName("기본은 영상만 쓴다 - 사진뿐인 여행은 400 (앱의 선택 화면이 영상만 다루기 때문)")
    void createPinlog_photosAreNotUsedByDefault() throws Exception {
        String token = signupAndLogin("pinlog3@pinkok.com", "pinlog3");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        writeDiaryWithMedia(token, addPin(token, tripId, dayId, "성산일출봉"), uploadPhoto(token));

        MvcResult created = createPinlog(token, String.format("{\"tripId\":%d}", tripId));
        assertThat(created.getResponse().getStatus()).isEqualTo(400);

        // 고를 거리 목록에도 사진은 안 나온다
        mockMvc.perform(get("/pinlogs/clip-options").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("includePhotos 를 켜면 영상이 없는 핀을 사진(정지 장면)으로 채운다 - 사진 슬라이드쇼 비상계획")
    void createPinlog_withIncludePhotos() throws Exception {
        String token = signupAndLogin("pinlog3b@pinkok.com", "pinlog3b");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        Long photoMedia = writeDiaryWithMedia(token, addPin(token, tripId, dayId, "성산일출봉"), uploadPhoto(token));

        MvcResult created = createPinlog(token,
                String.format("{\"tripId\":%d,\"includePhotos\":true}", tripId));
        assertThat(created.getResponse().getStatus()).isEqualTo(202);

        String json = created.getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(json, "$.clips[0].diaryMediaId")).isEqualTo(photoMedia.intValue());
        assertThat((String) JsonPath.read(json, "$.clips[0].mediaType")).isEqualTo("PHOTO");

        // 사진은 정지 장면으로 넘어가야 한다
        awaitFinalStatus(token, ((Number) JsonPath.read(json, "$.id")).longValue());
        assertThat(fakeVideoComposer.lastClips().get(0).photo()).isTrue();

        mockMvc.perform(get("/pinlogs/clip-options")
                        .param("tripId", tripId.toString())
                        .param("includePhotos", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].options[0].mediaType").value("PHOTO"));
    }

    @Test
    @DisplayName("clip-options - 핀마다 팀원들의 영상·사진이 작성자 정보와 함께 나온다")
    void clipOptions_listsTeamMedia() throws Exception {
        String owner = signupAndLogin("pinlog4@pinkok.com", "pinlog4");
        String mate = signupAndLogin("pinlog4mate@pinkok.com", "pinlog4mate");
        Long tripId = createTrip(owner);
        joinTrip(tripId, "pinlog4mate@pinkok.com");
        Long dayId = createDay(owner, tripId, 1);
        Long pin = addPin(owner, tripId, dayId, "협재해수욕장");

        writeDiaryWithMedia(owner, pin, uploadVideo(owner));
        writeDiaryWithMedia(mate, pin, uploadVideo(mate));

        mockMvc.perform(get("/pinlogs/clip-options").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placeName").value("협재해수욕장"))
                .andExpect(jsonPath("$[0].options.length()").value(2))
                // 내 영상이 먼저 보인다
                .andExpect(jsonPath("$[0].options[0].mine").value(true))
                .andExpect(jsonPath("$[0].options[0].author.nickname").value("pinlog4"))
                .andExpect(jsonPath("$[0].options[1].mine").value(false))
                .andExpect(jsonPath("$[0].options[1].author.nickname").value("pinlog4mate"));
    }

    @Test
    @DisplayName("다른 여행의 미디어를 섞어 보내면 400")
    void createPinlog_mediaFromAnotherTrip_returns400() throws Exception {
        String token = signupAndLogin("pinlog5@pinkok.com", "pinlog5");
        Long tripA = createTrip(token);
        Long tripB = createTrip(token);

        Long dayB = createDay(token, tripB, 1);
        Long mediaInB = writeDiaryWithMedia(token, addPin(token, tripB, dayB, "우도"), uploadVideo(token));

        Long dayA = createDay(token, tripA, 1);
        writeDiaryWithMedia(token, addPin(token, tripA, dayA, "협재"), uploadVideo(token));

        MvcResult result = createPinlog(token, String.format(
                "{\"tripId\":%d,\"clips\":[{\"diaryMediaId\":%d}]}", tripA, mediaInB));
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("팀원이 아니면 PinLog 를 만들지도 보지도 못한다 (403)")
    void nonMember_returns403() throws Exception {
        String owner = signupAndLogin("pinlog6@pinkok.com", "pinlog6");
        String stranger = signupAndLogin("pinlog6other@pinkok.com", "pinlog6other");
        Long tripId = createTrip(owner);
        Long dayId = createDay(owner, tripId, 1);
        writeDiaryWithMedia(owner, addPin(owner, tripId, dayId, "협재"), uploadVideo(owner));

        assertThat(createPinlog(stranger, String.format("{\"tripId\":%d}", tripId)).getResponse().getStatus())
                .isEqualTo(403);

        mockMvc.perform(get("/pinlogs").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/pinlogs/clip-options").param("tripId", tripId.toString())
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("합성이 실패하면 FAILED 와 실패 이유가 남는다")
    void composeFailure_isRecorded() throws Exception {
        String token = signupAndLogin("pinlog7@pinkok.com", "pinlog7");
        Long tripId = createTrip(token);
        Long dayId = createDay(token, tripId, 1);
        writeDiaryWithMedia(token, addPin(token, tripId, dayId, "협재"), uploadVideo(token));

        fakeVideoComposer.failNextWith("클립을 변환하지 못했습니다: 코덱 없음");

        MvcResult created = createPinlog(token, String.format("{\"tripId\":%d}", tripId));
        Long pinlogId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        String json = awaitFinalStatus(token, pinlogId);
        assertThat((String) JsonPath.read(json, "$.status")).isEqualTo("FAILED");
        assertThat((String) JsonPath.read(json, "$.errorMessage")).contains("코덱 없음");
        assertThat((Object) JsonPath.read(json, "$.videoUrl")).isNull();
    }

    @Test
    @DisplayName("삭제는 만든 사람만 할 수 있고, 합성된 영상 파일도 지워진다")
    void delete_onlyByCreator() throws Exception {
        String owner = signupAndLogin("pinlog8@pinkok.com", "pinlog8");
        String mate = signupAndLogin("pinlog8mate@pinkok.com", "pinlog8mate");
        Long tripId = createTrip(owner);
        joinTrip(tripId, "pinlog8mate@pinkok.com");
        Long dayId = createDay(owner, tripId, 1);
        String originalUrl = uploadVideo(owner);
        writeDiaryWithMedia(owner, addPin(owner, tripId, dayId, "협재"), originalUrl);

        MvcResult created = createPinlog(owner, String.format("{\"tripId\":%d}", tripId));
        Long pinlogId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
        String json = awaitFinalStatus(owner, pinlogId);
        String videoUrl = JsonPath.read(json, "$.videoUrl");
        assertThat(storedFile(videoUrl)).exists();

        // 팀원이어도 남이 만든 건 못 지운다
        mockMvc.perform(delete("/pinlogs/" + pinlogId).header("Authorization", "Bearer " + mate))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/pinlogs/" + pinlogId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        assertThat(storedFile(videoUrl)).doesNotExist();
        // 기록에 남긴 원본 영상은 그대로 있어야 한다
        assertThat(storedFile(originalUrl)).exists();
    }

    @Test
    @DisplayName("재료가 없거나 클립이 60개를 넘으면 400")
    void invalidClipCount_returns400() throws Exception {
        String token = signupAndLogin("pinlog9@pinkok.com", "pinlog9");
        Long tripId = createTrip(token);

        // 기록이 하나도 없는 여행
        MvcResult empty = createPinlog(token, String.format("{\"tripId\":%d}", tripId));
        assertThat(empty.getResponse().getStatus()).isEqualTo(400);

        Long dayId = createDay(token, tripId, 1);
        Long mediaId = writeDiaryWithMedia(token, addPin(token, tripId, dayId, "협재"), uploadVideo(token));

        StringBuilder clips = new StringBuilder();
        for (int i = 0; i < 61; i++) {
            if (i > 0) clips.append(",");
            clips.append(String.format("{\"diaryMediaId\":%d}", mediaId));
        }
        MvcResult tooMany = createPinlog(token,
                String.format("{\"tripId\":%d,\"clips\":[%s]}", tripId, clips));
        assertThat(tooMany.getResponse().getStatus()).isEqualTo(400);
        assertThat(tooMany.getResponse().getContentAsString()).contains("60개까지");
    }

    @Test
    @DisplayName("토큰 없이 부르면 401")
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/pinlogs").param("tripId", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/pinlogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":1}"))
                .andExpect(status().isUnauthorized());
    }
}
