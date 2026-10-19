package com.example.pinkok_backend.social;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * 카카오 로그인. 앱이 카카오 SDK 로 받은 <b>액세스 토큰</b>을 확인한다.
 *
 * <ol>
 *   <li>{@code GET /v1/user/access_token_info} — 토큰이 유효하고 <b>우리 앱(app_id)</b> 것인지</li>
 *   <li>{@code GET /v2/user/me} — 사용자 고유 ID, 닉네임, 이메일</li>
 * </ol>
 */
@Component
public class KakaoSocialClient implements SocialAuthClient {

    private final RestClient restClient;
    private final String appId;

    @Autowired
    public KakaoSocialClient(
            @Value("${kakao.api-base-url:https://kapi.kakao.com}") String baseUrl,
            @Value("${kakao.app-id:}") String appId) {
        this(RestClient.builder().baseUrl(baseUrl).build(), appId);
    }

    KakaoSocialClient(RestClient restClient, String appId) {
        this.restClient = restClient;
        this.appId = appId;
    }

    @Override
    public SocialProfile verify(String accessToken) {
        if (!StringUtils.hasText(appId)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "카카오 로그인이 설정되지 않았습니다. KAKAO_APP_ID 환경변수를 확인하세요.");
        }

        JsonNode info = get("/v1/user/access_token_info", accessToken);
        if (!appId.equals(text(info.path("app_id")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이 앱에서 발급된 카카오 토큰이 아닙니다.");
        }

        JsonNode me = get("/v2/user/me", accessToken);
        String uid = text(me.path("id"));
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 사용자 정보를 확인하지 못했습니다.");
        }

        JsonNode account = me.path("kakao_account");
        String nickname = text(account.path("profile").path("nickname"));
        if (nickname == null) {
            nickname = text(me.path("properties").path("nickname"));
        }
        boolean verified = account.path("is_email_valid").asBoolean(false)
                && account.path("is_email_verified").asBoolean(false);

        return new SocialProfile("KAKAO", uid, text(account.path("email")), verified, nickname);
    }

    private JsonNode get(String path, String accessToken) {
        try {
            JsonNode body = restClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 응답이 비어 있습니다.");
            }
            return body;
        } catch (HttpClientErrorException e) {
            // 토큰 만료·위조 등은 4xx 로 온다
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다. 다시 로그인해 주세요.", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 서버 호출에 실패했습니다.", e);
        }
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : null;
    }
}
