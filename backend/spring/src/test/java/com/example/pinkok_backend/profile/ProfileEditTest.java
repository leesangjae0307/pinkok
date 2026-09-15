package com.example.pinkok_backend.profile;

import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.repository.AvatarRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프로필 수정: 닉네임 변경, 프로필 사진 변경, 아바타와 프로필 사진은 둘 중 하나만 남는 규칙.
 * 업로드 파일은 테스트용 임시 폴더에 저장한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileEditTest {

    @TempDir
    static Path uploadDir;

    @DynamicPropertySource
    static void uploadDirProperty(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", () -> uploadDir.toString());
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AvatarRepository avatarRepository;

    private Long avatarId;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        avatarRepository.deleteAll();

        Avatar avatar = new Avatar();
        avatar.setCode("F1");
        avatar.setName("사진가");
        avatar.setImageUrl("https://cdn.pinkok.app/avatars/f1.png");
        avatar.setGender("FEMALE");
        avatar.setDisplayOrder(1);
        avatarId = avatarRepository.save(avatar).getId();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"profile@pinkok.com\",\"username\":\"profileuser\",\"password\":\"pass1234\",\"nickname\":\"처음이름\",\"avatarId\":%d}",
                                avatarId)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"profile@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    /** POST /files 로 테스트 사진을 올리고 원본 주소를 돌려준다. */
    private String uploadImage() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", png);

        MvcResult result = mockMvc.perform(multipart("/files")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", png.toByteArray()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$[0].url");
    }

    private Path storedFile(String url) {
        return uploadDir.resolve(url.substring("/files/".length()));
    }

    private Path storedThumbnail(String url) {
        String path = url.substring("/files/".length());
        return uploadDir.resolve(path.substring(0, path.lastIndexOf('.')) + "_thumb.jpg");
    }

    @Test
    @DisplayName("PATCH /users/me - 닉네임을 바꿀 수 있다")
    void updateNickname() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새이름\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새이름"));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.nickname").value("새이름"));
    }

    @Test
    @DisplayName("닉네임이 비어 있거나 50자를 넘으면 400")
    void updateNickname_invalid_returns400() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + "가".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("프로필 사진을 올리면 아바타가 해제된다")
    void updateProfileImage_clearsAvatar() throws Exception {
        String url = uploadImage();

        mockMvc.perform(patch("/users/me/profile-image")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileImageUrl\":\"" + url + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(url))
                .andExpect(jsonPath("$.avatar").doesNotExist());
    }

    @Test
    @DisplayName("아바타를 고르면 프로필 사진이 지워지고, 사진 파일도 폴더에서 삭제된다")
    void selectAvatar_clearsProfileImage() throws Exception {
        String url = uploadImage();
        mockMvc.perform(patch("/users/me/profile-image")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileImageUrl\":\"" + url + "\"}"))
                .andExpect(status().isOk());
        assertThat(storedFile(url)).exists();

        mockMvc.perform(patch("/users/me/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"avatarId\":%d}", avatarId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.code").value("F1"))
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist());

        assertThat(storedFile(url)).doesNotExist();
        assertThat(storedThumbnail(url)).doesNotExist();
    }

    @Test
    @DisplayName("프로필 사진을 다른 사진으로 바꾸면 예전 사진 파일은 삭제된다")
    void updateProfileImage_twice_deletesOldFile() throws Exception {
        String first = uploadImage();
        String second = uploadImage();

        for (String url : new String[]{first, second}) {
            mockMvc.perform(patch("/users/me/profile-image")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"profileImageUrl\":\"" + url + "\"}"))
                    .andExpect(status().isOk());
        }

        assertThat(storedFile(first)).doesNotExist();
        assertThat(storedFile(second)).exists();
    }

    @Test
    @DisplayName("우리 업로드 주소가 아니거나, 없는 파일이면 400")
    void updateProfileImage_invalidUrl_returns400() throws Exception {
        String[] invalid = {
                "https://example.com/cat.jpg",
                "/files/2026/09/not-exists.jpg",
                "/files/2026/09/clip.mp4"
        };
        for (String url : invalid) {
            mockMvc.perform(patch("/users/me/profile-image")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"profileImageUrl\":\"" + url + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        String url = uploadImage();
        mockMvc.perform(patch("/users/me/profile-image")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileImageUrl\":\"" + url.replace(".png", "_thumb.jpg") + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 없이 프로필을 수정하면 401")
    void updateProfile_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새이름\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/users/me/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileImageUrl\":\"/files/2026/09/a.jpg\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("임시 업로드 폴더가 테스트마다 실제로 쓰인다")
    void uploadDirIsUsed() throws Exception {
        String url = uploadImage();
        assertThat(Files.isRegularFile(storedFile(url))).isTrue();
    }
}
