package com.example.pinkok_backend.config;

import com.example.pinkok_backend.storage.LocalFileStorage;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 파일 하나 최대 크기. 영상(20MB)에 맞춘 상한이고, 사진(10MB)은 FileService 에서 따로 더 작게 막는다.
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    // 한 번에 여러 개 올릴 때 합계 최대 크기
    private static final long MAX_REQUEST_SIZE = 100L * 1024 * 1024;

    private final LocalFileStorage localFileStorage;

    public WebConfig(LocalFileStorage localFileStorage) {
        this.localFileStorage = localFileStorage;
    }

    /**
     * /files/** 주소로 들어온 요청을 uploads 폴더의 실제 파일로 연결한다.
     * 예) /files/2026/09/abc.jpg -> uploads/2026/09/abc.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Files.createDirectories(localFileStorage.getRootDir());
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 폴더를 만들 수 없습니다", e);
        }

        String location = localFileStorage.getRootDir().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler(LocalFileStorage.URL_PREFIX + "**")
                .addResourceLocations(location);
    }

    /**
     * 업로드 용량 제한. 스프링 기본값은 1MB라 사진도 막히므로 늘려둔다.
     * application.properties 는 깃에 안 올라가서, 팀원 모두 같은 설정을 쓰도록 코드에 둔다.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", MAX_FILE_SIZE, MAX_REQUEST_SIZE, 0);
    }
}
