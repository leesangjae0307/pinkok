package com.example.pinkok_backend.storage;

import com.example.pinkok_backend.support.DatabaseCleaner;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 배포 설정(file.storage=gcs)으로 서버를 띄웠을 때 업로드 → 기록에 첨부 → 기록 삭제까지
 * 파일이 버킷에 올라가고 지워지는지 확인한다. 진짜 버킷 대신 메모리 가짜 저장소를 끼운다.
 */
@SpringBootTest(properties = {"file.storage=gcs", "gcs.bucket=pinkok-test"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GcsUploadTest {

    @TestConfiguration
    static class FakeBucket {
        @Bean
        @Primary
        Storage fakeStorage() {
            return LocalStorageHelper.getOptions().getService();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    Storage storage;

    @Autowired
    FileStorage fileStorage;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    private String signupAndLogin() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gcs@pinkok.com\",\"username\":\"gcsuser\",\"password\":\"pass1234\",\"nickname\":\"지은\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gcs@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private Long createId(String token, String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private boolean inBucket(String url) {
        String path = url.substring("https://storage.googleapis.com/pinkok-test/".length());
        return storage.get(BlobId.of("pinkok-test", path)) != null;
    }

    @Test
    @DisplayName("gcs 설정이면 사진이 버킷에 올라가고, 기록을 지우면 버킷에서도 지워진다")
    void uploadAttachDelete() throws Exception {
        assertThat(fileStorage).isInstanceOf(GcsFileStorage.class);
        String token = signupAndLogin();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", png);
        MvcResult upload = mockMvc.perform(multipart("/files")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", png.toByteArray()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String json = upload.getResponse().getContentAsString();
        String url = JsonPath.read(json, "$[0].url");
        String thumbnailUrl = JsonPath.read(json, "$[0].thumbnailUrl");

        // 앱이 바로 열 수 있는 버킷 공개 주소로 온다. 원본과 썸네일 둘 다 버킷에 있다
        assertThat(url).startsWith("https://storage.googleapis.com/pinkok-test/");
        assertThat(inBucket(url)).isTrue();
        assertThat(inBucket(thumbnailUrl)).isTrue();

        // 기록에 첨부할 때 "우리 저장소에 있는 파일인지" 확인도 버킷 기준으로 된다
        Long tripId = createId(token, "/trips", "{\"title\":\"제주\"}");
        Long pinId = createId(token, "/itinerary-items", String.format(
                "{\"tripId\":%d,\"place\":{\"name\":\"협재\",\"latitude\":33.45,\"longitude\":126.57}}", tripId));
        Long diaryId = createId(token, "/diaries", String.format(
                "{\"itineraryItemId\":%d,\"content\":\"좋았다\",\"media\":[{\"fileUrl\":\"%s\"}]}", pinId, url));

        mockMvc.perform(delete("/diaries/" + diaryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(inBucket(url)).isFalse();
    }

    @Test
    @DisplayName("gcs 설정이면 /files/** 로컬 주소는 더 이상 열리지 않는다 (파일은 버킷 주소로만)")
    void noLocalFilesRoute() throws Exception {
        mockMvc.perform(get("/files/2026/11/anything.jpg")).andExpect(status().isNotFound());
    }
}
