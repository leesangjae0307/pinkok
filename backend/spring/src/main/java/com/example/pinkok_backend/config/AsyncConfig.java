package com.example.pinkok_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AI 요청은 Gemini 응답을 몇 초~수십 초 기다려야 해서, 요청 스레드를 붙잡지 않고
 * 별도 스레드 풀에서 처리한다 (AiRequestService 참고).
 */
@Configuration
public class AsyncConfig {

    @Bean
    public TaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-request-");
        executor.initialize();
        return executor;
    }
}
