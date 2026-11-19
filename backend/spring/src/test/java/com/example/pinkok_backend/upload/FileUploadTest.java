package com.example.pinkok_backend.upload;

import com.example.pinkok_backend.pinlog.HeicConverter;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    /** 윈도우에는 heif-convert 가 없어서 가짜로 바꾼다. 실제 변환은 배포 서버와 같은 Docker 이미지에서 확인했다 */
    @MockitoBean
    HeicConverter heicConverter;

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
    @DisplayName("세로로 찍은 폰 사진(EXIF 회전 6): 크기는 보이는 방향(세로)으로, 썸네일도 돌려서 만든다")
    void exifRotatedPhoto() throws Exception {
        // 픽셀은 가로 800×400 (왼쪽 빨강·오른쪽 파랑), EXIF 는 "시계방향 90도로 보여줘라"
        byte[] photo = com.example.pinkok_backend.pinlog.ExifTestImages.leftRedRightBlue(800, 400, 6);
        MvcResult result = upload(new MockMultipartFile("file", "portrait.jpg", "image/jpeg", photo));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String json = result.getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(json, "$[0].width")).isEqualTo(400);    // 보이는 방향: 세로 사진
        assertThat((Integer) JsonPath.read(json, "$[0].height")).isEqualTo(800);
        // 원본은 손대지 않는다 (앱·브라우저가 EXIF 를 보고 돌려서 보여준다)
        assertThat(Files.readAllBytes(stored(JsonPath.read(json, "$[0].url")))).isEqualTo(photo);

        BufferedImage thumbnail = ImageIO.read(stored(JsonPath.read(json, "$[0].thumbnailUrl")).toFile());
        assertThat(thumbnail.getWidth()).isEqualTo(300);
        assertThat(thumbnail.getHeight()).isEqualTo(600);
        // 시계방향으로 돌렸으니 왼쪽(빨강)이 위, 오른쪽(파랑)이 아래
        assertThat(com.example.pinkok_backend.pinlog.ExifTestImages.isRed(thumbnail.getRGB(150, 100))).isTrue();
        assertThat(com.example.pinkok_backend.pinlog.ExifTestImages.isBlue(thumbnail.getRGB(150, 500))).isTrue();
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

    /** FFmpeg 로 WebP 사진을 만든다 (인스타그램 사진과 같은 형식). libwebp 가 없으면 테스트를 건너뛴다 */
    private static byte[] webp(int width, int height, String color) throws Exception {
        Assumptions.assumeTrue(commandExists("ffmpeg") && commandExists("ffprobe"),
                "FFmpeg 가 설치돼 있지 않아 건너뜁니다.");
        Path file = Files.createTempFile("photo-", ".webp");
        try {
            Process process = new ProcessBuilder("ffmpeg", "-y", "-f", "lavfi",
                    "-i", "color=c=" + color + ":s=" + width + "x" + height + ",format=rgba",
                    "-frames:v", "1", "-c:v", "libwebp", file.toString())
                    .redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            Assumptions.assumeTrue(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0,
                    "FFmpeg 에 WebP 인코더가 없어 건너뜁니다.");
            return Files.readAllBytes(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("인스타그램 사진처럼 이름은 .jpg 인데 실제로는 WebP 면, 원본은 WebP 그대로 저장하고 썸네일은 JPEG 로 만든다")
    void webpNamedJpg() throws Exception {
        byte[] photo = webp(1080, 1350, "red");
        MvcResult result = upload(new MockMultipartFile("file", "upload-1794964907090-1.jpg", "image/jpeg", photo));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String json = result.getResponse().getContentAsString();
        String url = JsonPath.read(json, "$[0].url");
        assertThat(url).endsWith(".webp");                       // 실제 형식의 확장자로 저장
        assertThat(Files.readAllBytes(stored(url))).isEqualTo(photo); // 원본은 변환 없이 그대로 (화질 유지)
        assertThat((String) JsonPath.read(json, "$[0].mediaType")).isEqualTo("IMAGE");
        assertThat((Integer) JsonPath.read(json, "$[0].width")).isEqualTo(1080);
        assertThat((Integer) JsonPath.read(json, "$[0].height")).isEqualTo(1350);

        BufferedImage thumbnail = ImageIO.read(stored(JsonPath.read(json, "$[0].thumbnailUrl")).toFile());
        assertThat(thumbnail).isNotNull();                       // 썸네일은 자바도 읽을 수 있는 JPEG
        assertThat(thumbnail.getWidth()).isEqualTo(300);
        assertThat(thumbnail.getHeight()).isBetween(374, 376);
    }

    @Test
    @DisplayName("투명 배경 WebP 의 썸네일은 검게 되지 않고 흰 바탕 위에 그려진다")
    void transparentWebpThumbnail() throws Exception {
        byte[] photo = webp(400, 400, "black@0.0");               // 완전히 투명한 사진
        String json = upload(new MockMultipartFile("file", "clear.webp", "image/webp", photo))
                .getResponse().getContentAsString();

        BufferedImage thumbnail = ImageIO.read(stored(JsonPath.read(json, "$[0].thumbnailUrl")).toFile());
        int rgb = thumbnail.getRGB(thumbnail.getWidth() / 2, thumbnail.getHeight() / 2);
        assertThat((rgb >> 16) & 0xFF).isGreaterThan(200);       // 흰색에 가깝다 (검은색이면 0 근처)
        assertThat((rgb >> 8) & 0xFF).isGreaterThan(200);
        assertThat(rgb & 0xFF).isGreaterThan(200);
    }

    @Test
    @DisplayName("이름이 .jpg 인 PNG 는 .png 로 저장한다 (이름이 아니라 내용으로 판단)")
    void pngNamedJpg() throws Exception {
        MockMultipartFile disguised = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
                png("x.png", 200, 100).getBytes());
        String json = upload(disguised).getResponse().getContentAsString();
        assertThat((String) JsonPath.read(json, "$[0].url")).endsWith(".png");
        assertThat((Integer) JsonPath.read(json, "$[0].width")).isEqualTo(200);
    }

    /** 배포 서버와 같은 libheif 로 실제로 만든 HEIC 사진과, heif-convert 로 바꾼 결과 (1200×900) */
    private static byte[] fixture(String name) throws IOException {
        try (InputStream in = FileUploadTest.class.getResourceAsStream("/fixtures/" + name)) {
            assertThat(in).as(name).isNotNull();
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("아이폰 HEIC 사진은 JPEG 로 바꿔서 저장한다 (이후 썸네일 등은 일반 JPEG 와 같다)")
    void heicConvertedToJpeg() throws Exception {
        byte[] heic = fixture("sample.heic");
        byte[] converted = fixture("sample-from-heic.jpg");
        when(heicConverter.toJpeg(heic)).thenReturn(Optional.of(converted));

        MvcResult result = upload(new MockMultipartFile("file", "IMG_0001.HEIC", "image/heic", heic));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String json = result.getResponse().getContentAsString();
        String url = JsonPath.read(json, "$[0].url");
        assertThat(url).endsWith(".jpg");
        assertThat(Files.readAllBytes(stored(url))).isEqualTo(converted);
        assertThat((Integer) JsonPath.read(json, "$[0].width")).isEqualTo(1200);
        assertThat((Integer) JsonPath.read(json, "$[0].height")).isEqualTo(900);
        assertThat(((Number) JsonPath.read(json, "$[0].size")).longValue()).isEqualTo(converted.length);
        assertThat(ImageIO.read(stored(JsonPath.read(json, "$[0].thumbnailUrl")).toFile()).getWidth()).isEqualTo(300);
    }

    @Test
    @DisplayName("이름이 .jpg 여도 내용이 HEIC 면 변환한다 (이름이 아니라 내용으로 판단)")
    void heicNamedJpg() throws Exception {
        byte[] heic = fixture("sample.heic");
        when(heicConverter.toJpeg(heic)).thenReturn(Optional.of(fixture("sample-from-heic.jpg")));

        String json = upload(new MockMultipartFile("file", "upload-1.jpg", "image/jpeg", heic))
                .getResponse().getContentAsString();
        assertThat((String) JsonPath.read(json, "$[0].url")).endsWith(".jpg");
        verify(heicConverter).toJpeg(heic);
    }

    @Test
    @DisplayName("HEIC 변환 도구가 없거나 실패하면 무엇이 문제인지 알 수 있는 메시지로 거절한다")
    void rejectsHeicWhenConversionFails() throws Exception {
        when(heicConverter.toJpeg(any())).thenReturn(Optional.empty());

        MvcResult result = upload(new MockMultipartFile("file", "IMG_0001.HEIC", "image/heic", fixture("sample.heic")));
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("HEIC");
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
