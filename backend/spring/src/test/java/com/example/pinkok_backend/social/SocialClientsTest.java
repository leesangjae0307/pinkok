package com.example.pinkok_backend.social;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 카카오·구글 토큰 검증 (제공자 서버는 가짜로 대신한다). */
class SocialClientsTest {

    // ---------------- 카카오 ----------------

    private record KakaoFixture(KakaoSocialClient client, MockRestServiceServer server) {
    }

    private KakaoFixture kakao(String appId) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://kapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new KakaoFixture(new KakaoSocialClient(builder.build(), appId), server);
    }

    private static final String KAKAO_ME = """
            {"id": 3141592, "kakao_account": {"profile": {"nickname": "카카오닉"},
             "email": "kakao@example.com", "is_email_valid": true, "is_email_verified": true}}""";

    @Test
    @DisplayName("카카오 - 우리 앱 토큰이면 사용자 정보를 돌려준다 (토큰은 Bearer 로 전달)")
    void kakao_success() {
        KakaoFixture f = kakao("1234");
        f.server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("{\"id\":3141592,\"app_id\":1234,\"expires_in\":3000}", MediaType.APPLICATION_JSON));
        f.server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess(KAKAO_ME, MediaType.APPLICATION_JSON));

        SocialProfile p = f.client.verify("tok");

        assertEquals("KAKAO", p.provider());
        assertEquals("3141592", p.uid());
        assertEquals("kakao@example.com", p.email());
        assertTrue(p.emailVerified());
        assertEquals("카카오닉", p.nickname());
        f.server.verify();
    }

    @Test
    @DisplayName("카카오 - 다른 앱에서 발급된 토큰(app_id 불일치)이면 401, 사용자 정보는 조회조차 안 한다")
    void kakao_wrongApp_returns401() {
        KakaoFixture f = kakao("1234");
        f.server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withSuccess("{\"id\":1,\"app_id\":9999}", MediaType.APPLICATION_JSON));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> f.client.verify("tok"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        f.server.verify(); // /v2/user/me 호출 기대가 없으므로 호출했다면 여기서 실패
    }

    @Test
    @DisplayName("카카오 - 카카오가 토큰을 거부하면(만료·위조) 401")
    void kakao_rejectedToken_returns401() {
        KakaoFixture f = kakao("1234");
        f.server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> f.client.verify("bad"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }

    @Test
    @DisplayName("카카오 - 카카오 서버가 장애면 502")
    void kakao_providerDown_returns502() {
        KakaoFixture f = kakao("1234");
        f.server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> f.client.verify("tok"));

        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatusCode());
    }

    @Test
    @DisplayName("카카오 - 앱 ID 설정이 없으면 503 (검증 없이 통과시키지 않는다)")
    void kakao_notConfigured_returns503() {
        KakaoFixture f = kakao("");

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> f.client.verify("tok"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
    }

    @Test
    @DisplayName("카카오 - 이메일 동의가 없으면 email 은 null, 이메일이 미인증이면 verified=false")
    void kakao_noEmail_andUnverified() {
        KakaoFixture noEmail = kakao("1234");
        noEmail.server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withSuccess("{\"app_id\":1234}", MediaType.APPLICATION_JSON));
        noEmail.server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess("{\"id\":7,\"properties\":{\"nickname\":\"옛닉\"}}", MediaType.APPLICATION_JSON));
        SocialProfile p = noEmail.client.verify("tok");
        assertNull(p.email());
        assertFalse(p.emailVerified());
        assertEquals("옛닉", p.nickname());

        KakaoFixture unverified = kakao("1234");
        unverified.server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withSuccess("{\"app_id\":1234}", MediaType.APPLICATION_JSON));
        unverified.server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess("{\"id\":8,\"kakao_account\":{\"email\":\"x@y.com\",\"is_email_valid\":true,\"is_email_verified\":false}}",
                        MediaType.APPLICATION_JSON));
        assertFalse(unverified.client.verify("tok").emailVerified());
    }

    // ---------------- 구글 ----------------

    private record GoogleFixture(GoogleSocialClient client, MockRestServiceServer server) {
    }

    private GoogleFixture google(String clientIds) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth2.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new GoogleFixture(new GoogleSocialClient(builder.build(), clientIds), server);
    }

    private static String googleInfo(String aud, String iss) {
        return "{\"aud\":\"" + aud + "\",\"iss\":\"" + iss + "\",\"sub\":\"1122334455\",\"email\":\"g@example.com\","
                + "\"email_verified\":\"true\",\"name\":\"구글닉\"}";
    }

    @Test
    @DisplayName("구글 - 우리 클라이언트 ID 로 발급된 토큰이면 사용자 정보를 돌려준다 (email_verified 가 문자열이어도 처리)")
    void google_success() {
        GoogleFixture f = google("android-id, web-id");
        f.server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=abc"))
                .andExpect(queryParam("id_token", "abc"))
                .andRespond(withSuccess(googleInfo("web-id", "https://accounts.google.com"), MediaType.APPLICATION_JSON));

        SocialProfile p = f.client.verify("abc");

        assertEquals("GOOGLE", p.provider());
        assertEquals("1122334455", p.uid());
        assertEquals("g@example.com", p.email());
        assertTrue(p.emailVerified());
        assertEquals("구글닉", p.nickname());
    }

    @Test
    @DisplayName("구글 - 다른 앱 클라이언트 ID(aud 불일치)면 401")
    void google_wrongAudience_returns401() {
        GoogleFixture f = google("my-id");
        f.server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=abc"))
                .andRespond(withSuccess(googleInfo("someone-elses-id", "accounts.google.com"), MediaType.APPLICATION_JSON));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> f.client.verify("abc"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }

    @Test
    @DisplayName("구글 - 발급자가 구글이 아니면 401")
    void google_wrongIssuer_returns401() {
        GoogleFixture f = google("my-id");
        f.server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=abc"))
                .andRespond(withSuccess(googleInfo("my-id", "https://evil.example.com"), MediaType.APPLICATION_JSON));

        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> f.client.verify("abc")).getStatusCode());
    }

    @Test
    @DisplayName("구글 - 구글이 토큰을 거부하면(만료·위조 400) 401")
    void google_rejectedToken_returns401() {
        GoogleFixture f = google("my-id");
        f.server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=bad"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> f.client.verify("bad")).getStatusCode());
    }

    @Test
    @DisplayName("구글 - 클라이언트 ID 설정이 없으면 503, 구글 장애면 502")
    void google_notConfigured_andProviderDown() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class, () -> google("").client.verify("abc")).getStatusCode());

        GoogleFixture down = google("my-id");
        down.server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=abc"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals(HttpStatus.BAD_GATEWAY,
                assertThrows(ResponseStatusException.class, () -> down.client.verify("abc")).getStatusCode());
    }
}
