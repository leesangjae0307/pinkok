package com.example.pinkok_backend.push;

import com.example.pinkok_backend.repository.PushTokenRepository;
import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 폰 푸시. 진짜 Firebase 는 부르지 않고 {@link PushSender} 를 가짜로 바꿔서
 * "누구의 어떤 토큰으로 무슨 내용을 보내려 했는지"만 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PushNotificationTest {

    private static final String PHONE_A = "fcm-token-phone-a";
    private static final String PHONE_B = "fcm-token-phone-b";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    PushTokenRepository pushTokenRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    PushSender pushSender;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        when(pushSender.send(anyList(), any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    // ---------- 준비용 도우미 ----------

    private String signupAndLogin(String username, String nickname) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s@pinkok.com\",\"username\":\"%s\",\"password\":\"pass1234\",\"nickname\":\"%s\"}",
                                username, username, nickname)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s@pinkok.com\",\"password\":\"pass1234\"}", username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private void registerToken(String accessToken, String fcmToken) throws Exception {
        mockMvc.perform(post("/users/me/push-tokens")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"token\":\"%s\",\"platform\":\"ANDROID\"}", fcmToken)))
                .andExpect(status().isNoContent());
    }

    private void inviteToNewTrip(String ownerToken, String inviteeUsername) throws Exception {
        mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"title\":\"제주도 여행\",\"region\":\"제주\",\"inviteeUsernames\":[\"%s\"]}",
                                inviteeUsername)))
                .andExpect(status().isCreated());
    }

    private String ownerOf(String fcmToken) {
        // 엔티티의 user 는 지연 로딩이라 트랜잭션 밖에서 못 읽는다. SQL 로 바로 확인한다.
        return jdbcTemplate.queryForObject(
                "SELECT u.username FROM push_tokens p JOIN users u ON u.id = p.user_id WHERE p.token = ?",
                String.class, fcmToken);
    }

    /** 푸시는 별도 스레드에서 나가므로 결과가 DB 에 반영될 때까지 잠깐 기다린다. */
    private void waitUntil(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 50 && !condition.getAsBoolean(); i++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    // ---------- 테스트 ----------

    @Test
    @DisplayName("초대받으면 그 사람의 폰으로 푸시가 간다 — 눌렀을 때 갈 화면 정보도 함께")
    @SuppressWarnings("unchecked")
    void invitationSendsPush() throws Exception {
        String owner = signupAndLogin("gamjieun", "감지은");
        String mate = signupAndLogin("jeonghun", "박정훈");
        registerToken(mate, PHONE_A);

        inviteToNewTrip(owner, "jeonghun");

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<PushSender.Message> message = ArgumentCaptor.forClass(PushSender.Message.class);
        verify(pushSender, timeout(5000)).send(tokens.capture(), message.capture());

        assertThat(tokens.getValue()).containsExactly(PHONE_A);
        assertThat(message.getValue().title()).contains("제주도 여행");
        assertThat(message.getValue().data())
                .containsEntry("type", "TRIP_INVITED")
                .containsEntry("targetType", "INVITATION")
                .containsKeys("targetId", "notificationId");
    }

    @Test
    @DisplayName("설정에서 알림을 끄면 푸시는 안 가지만, 앱 안 알림함에는 남는다")
    void disabledNotificationSkipsPush() throws Exception {
        String owner = signupAndLogin("gamjieun", "감지은");
        String mate = signupAndLogin("jeonghun", "박정훈");
        registerToken(mate, PHONE_A);
        mockMvc.perform(patch("/users/me/settings")
                        .header("Authorization", "Bearer " + mate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationEnabled\":false}"))
                .andExpect(status().isOk());

        inviteToNewTrip(owner, "jeonghun");

        verify(pushSender, after(1000).never()).send(anyList(), any());
        String notifications = mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + mate))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<?>) JsonPath.read(notifications, "$")).hasSize(1);
    }

    @Test
    @DisplayName("FCM 이 못 쓰는 토큰이라고 하면(앱 삭제 등) 그 토큰은 지운다")
    void deadTokenIsRemoved() throws Exception {
        String owner = signupAndLogin("gamjieun", "감지은");
        String mate = signupAndLogin("jeonghun", "박정훈");
        registerToken(mate, PHONE_A);
        registerToken(mate, PHONE_B);
        when(pushSender.send(anyList(), any())).thenReturn(List.of(PHONE_A));

        inviteToNewTrip(owner, "jeonghun");

        waitUntil(() -> pushTokenRepository.findByToken(PHONE_A).isEmpty());
        assertThat(pushTokenRepository.findByToken(PHONE_B)).isPresent();
    }

    @Test
    @DisplayName("같은 폰에서 다른 계정으로 로그인해 등록하면 토큰 주인이 바뀐다")
    void sameTokenMovesToNewUser() throws Exception {
        String jieun = signupAndLogin("gamjieun", "감지은");
        String jeonghun = signupAndLogin("jeonghun", "박정훈");

        registerToken(jieun, PHONE_A);
        registerToken(jieun, PHONE_A); // 여러 번 보내도 하나만
        assertThat(pushTokenRepository.count()).isEqualTo(1);
        assertThat(ownerOf(PHONE_A)).isEqualTo("gamjieun");

        registerToken(jeonghun, PHONE_A);
        assertThat(pushTokenRepository.count()).isEqualTo(1);
        assertThat(ownerOf(PHONE_A)).isEqualTo("jeonghun");
    }

    @Test
    @DisplayName("로그아웃할 때 해제하면 지워진다. 남의 토큰은 해제할 수 없다")
    void unregister() throws Exception {
        String jieun = signupAndLogin("gamjieun", "감지은");
        String jeonghun = signupAndLogin("jeonghun", "박정훈");
        registerToken(jieun, PHONE_A);

        // 남이 지우려 하면 조용히 무시 (204 지만 그대로 남는다)
        mockMvc.perform(delete("/users/me/push-tokens")
                        .header("Authorization", "Bearer " + jeonghun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + PHONE_A + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(pushTokenRepository.findByToken(PHONE_A)).isPresent();

        mockMvc.perform(delete("/users/me/push-tokens")
                        .header("Authorization", "Bearer " + jieun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + PHONE_A + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(pushTokenRepository.findByToken(PHONE_A)).isEmpty();
    }

    @Test
    @DisplayName("탈퇴하면 그 사람의 기기 토큰도 모두 지워진다")
    void withdrawRemovesTokens() throws Exception {
        String jieun = signupAndLogin("gamjieun", "감지은");
        registerToken(jieun, PHONE_A);
        registerToken(jieun, PHONE_B);

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + jieun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"pass1234\"}"))
                .andExpect(status().is2xxSuccessful());

        assertThat(pushTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("로그인 없이 등록하면 401, 토큰이 비었거나 platform 이 이상하면 400")
    void validation() throws Exception {
        mockMvc.perform(post("/users/me/push-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + PHONE_A + "\"}"))
                .andExpect(status().isUnauthorized());

        String jieun = signupAndLogin("gamjieun", "감지은");
        mockMvc.perform(post("/users/me/push-tokens")
                        .header("Authorization", "Bearer " + jieun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/users/me/push-tokens")
                        .header("Authorization", "Bearer " + jieun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + PHONE_A + "\",\"platform\":\"WINDOWS\"}"))
                .andExpect(status().isBadRequest());
    }
}
