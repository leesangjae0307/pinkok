package com.example.pinkok_backend.social;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;

/**
 * 구글 로그인. 앱이 구글 로그인 SDK 로 받은 <b>ID 토큰</b>을 확인한다.
 *
 * <p>구글의 {@code tokeninfo} 엔드포인트가 서명·만료를 검증해 주고, 우리는 그 결과에서
 * <b>aud(이 토큰을 받을 앱)가 우리 클라이언트 ID 인지</b>, 발급자가 구글인지 확인한다.
 */
@Component
public class GoogleSocialClient implements SocialAuthClient {

    private static final List<String> ISSUERS = List.of("accounts.google.com", "https://accounts.google.com");

    private final RestClient restClient;
    private final List<String> clientIds;

    @Autowired
    public GoogleSocialClient(
            @Value("${google.token-info-base-url:https://oauth2.googleapis.com}") String baseUrl,
            @Value("${google.client-ids:}") String clientIds) {
        this(RestClient.builder().baseUrl(baseUrl).build(), clientIds);
    }

    GoogleSocialClient(RestClient restClient, String clientIds) {
        this.restClient = restClient;
        // 안드로이드·iOS·웹 클라이언트 ID 가 각각 다를 수 있어서 쉼표로 여러 개를 받는다
        this.clientIds = Arrays.stream(clientIds.split(","))
                .map(String::trim).filter(StringUtils::hasText).toList();
    }

    @Override
    public SocialProfile verify(String idToken) {
        if (clientIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "구글 로그인이 설정되지 않았습니다. GOOGLE_CLIENT_IDS 환경변수를 확인하세요.");
        }

        JsonNode info;
        try {
            info = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글 인증에 실패했습니다. 다시 로그인해 주세요.", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "구글 서버 호출에 실패했습니다.", e);
        }
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "구글 응답이 비어 있습니다.");
        }

        if (!clientIds.contains(text(info.path("aud")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이 앱에서 발급된 구글 토큰이 아닙니다.");
        }
        if (!ISSUERS.contains(text(info.path("iss")))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글에서 발급한 토큰이 아닙니다.");
        }
        String uid = text(info.path("sub"));
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글 사용자 정보를 확인하지 못했습니다.");
        }

        // tokeninfo 는 email_verified 를 문자열("true")로 주기도 한다
        boolean verified = "true".equalsIgnoreCase(text(info.path("email_verified")));
        return new SocialProfile("GOOGLE", uid, text(info.path("email")), verified, text(info.path("name")));
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : null;
    }
}
