package com.example.pinkok_backend.upload;

import com.example.pinkok_backend.storage.FileStorage;
import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 파일 업로드. 원본·썸네일 저장을 동시에 돌리도록 바꾼 뒤에도 결과(주소·크기·썸네일)가 그대로인지,
 * 하나라도 실패하면 이미 저장한 파일까지 깨끗이 지우는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadTest {

    @TempDir
    static Path uploadDir;

    @DynamicPropertySource
    static void uploadDir(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", () -> uploadDir.toString());
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @MockitoSpyBean
    FileStorage fileStorage;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        clearUploadDir();
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"up@pinkok.com\",\"username\":\"uploader\",\"password\":\"pass1234\",\"nickname\":\"업로더\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"up@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    @AfterEach
    void tearDown() throws IOException {
        databaseCleaner.clean();
        clearUploadDir();
    }

    // ---------- 도우미 ----------

    private static MockMultipartFile png(String name, int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile("file", name, "image/png", out.toByteArray());
    }

    private MvcResult upload(MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/files");
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        return mockMvc.perform(request.header("Authorization", "Bearer " + token)).andReturn();
    }

    private Path stored(String url) {
        return uploadDir.resolve(url.substring("/files/".length()));
    }

    private List<Path> storedFiles() throws IOException {
        if (!Files.exists(uploadDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(uploadDir)) {
            return files.filter(Files::isRegularFile).toList();
        }
    }

    private void clearUploadDir() throws IOException {
        for (Path file : storedFiles()) {
            Files.deleteIfExists(file);
        }
    }

    // ---------- 테스트 ----------

    @Test
    @DisplayName("큰 사진도 원본 크기는 정확히 알려주고, 썸네일은 가로 300px·같은 비율로 만든다")
    void bigPhoto() throws Exception {
        MvcResult result = upload(png("big.png", 4000, 3000));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String json = result.getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(json, "$[0].width")).isEqualTo(4000);
        assertThat((Integer) JsonPath.read(json, "$[0].height")).isEqualTo(3000);

        BufferedImage thumbnail = ImageIO.read(stored(JsonPath.read(json, "$[0].thumbnailUrl")).toFile());
        assertThat(thumbnail.getWidth()).isEqualTo(300);
        assertThat(thumbnail.getHeight()).isEqualTo(225);
        assertThat(Files.exists(stored(JsonPath.read(json, "$[0].url")))).isTrue();
    }

    @Test
    @DisplayName("작은 사진은 썸네일도 원래 크기 그대로")
    void smallPhoto() throws Exception {
        String json = upload(png("small.png", 120, 80)).getResponse().getContentAsString();
        BufferedImage thumbnail = ImageIO.read(stored(JsonPath.read(json, "$[0].thumbnailUrl")).toFile());
        assertThat(thumbnail.getWidth()).isEqualTo(120);
        assertThat(thumbnail.getHeight()).isEqualTo(80);
    }

    @Test
    @DisplayName("여러 장을 동시에 처리해도 응답 순서는 올린 순서 그대로")
    void keepsOrder() throws Exception {
        String json = upload(png("a.png", 1000, 500), png("b.png", 500, 1000), png("c.png", 700, 700))
                .getResponse().getContentAsString();

        assertThat((List<Integer>) JsonPath.read(json, "$[*].width")).containsExactly(1000, 500, 700);
        assertThat((List<Integer>) JsonPath.read(json, "$[*].height")).containsExactly(500, 1000, 700);
        assertThat(storedFiles()).hasSize(6); // 원본 3 + 썸네일 3
    }

    @Test
    @DisplayName("저장 도중 하나라도 실패하면 이미 저장된 원본·썸네일까지 모두 지운다")
    void rollsBackOnFailure() throws Exception {
        // 두 번째 사진의 썸네일 저장만 실패하게 만든다 (나머지는 동시에 저장되고 있는 중)
        doThrow(new IOException("저장소 오류")).when(fileStorage)
                .save(any(InputStream.class), argThat(path -> path != null && path.endsWith("_thumb.jpg")));

        MvcResult result = upload(png("a.png", 400, 300), png("b.png", 400, 300));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("가짜 사진(확장자만 png)은 아무것도 저장하지 않고 거절한다")
    void rejectsFakeImage() throws Exception {
        MvcResult result = upload(new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes()));
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("짧은 영상(1초 미만)도 FFmpeg 를 한 번에 돌려 썸네일과 길이를 받는다")
    void shortVideo() throws Exception {
        Assumptions.assumeTrue(commandExists("ffmpeg") && commandExists("ffprobe"),
                "FFmpeg 가 설치돼 있지 않아 건너뜁니다.");

        Path video = Files.createTempFile("clip-", ".mp4");
        try {
            run(List.of("ffmpeg", "-y", "-f", "lavfi", "-i", "testsrc=duration=0.8:size=320x240:rate=30",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p", video.toString()));

            String json = upload(new MockMultipartFile("file", "clip.mp4", "video/mp4", Files.readAllBytes(video)))
                    .getResponse().getContentAsString();

            assertThat((String) JsonPath.read(json, "$[0].mediaType")).isEqualTo("VIDEO");
            String thumbnailUrl = JsonPath.read(json, "$[0].thumbnailUrl");
            assertThat(thumbnailUrl).isNotNull();
            assertThat(Files.size(stored(thumbnailUrl))).isPositive();
            assertThat((Integer) JsonPath.read(json, "$[0].durationMs")).isBetween(600, 1000);
        } finally {
            Files.deleteIfExists(video);
        }
    }

    private static boolean commandExists(String command) {
        try {
            Process process = new ProcessBuilder(command, "-version").redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        assertThat(process.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).isZero();
    }
}
