package com.example.pinkok_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(springdoc) 설정. 서버 실행 후:
 *  - 문서 화면: /swagger-ui.html
 *  - 원본 JSON: /v3/api-docs
 *
 * 인증이 필요한 API를 화면에서 바로 테스트하려면, 오른쪽 위 "Authorize" 버튼을
 * 누르고 accessToken 값만 넣으면 된다 ("Bearer " 접두사는 자동으로 붙음).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI pinkokOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PinKok API")
                        .description("PinKok 백엔드 API 명세. 인증이 필요한 API는 먼저 /auth/login 으로 " +
                                "accessToken 을 받은 뒤, 오른쪽 위 Authorize 버튼에 넣어서 테스트하세요.")
                        .version("v0.1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
