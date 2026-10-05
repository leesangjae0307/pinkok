package com.example.pinkok_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * PinLog 영상 합성용 스레드 풀.
 *
 * <p>AI 요청용 풀(aiTaskExecutor)과 따로 둔다. 영상 합성은 CPU를 길게 쓰는 작업이라
 * 같은 풀을 쓰면 합성이 도는 동안 AI 장소 추출이 줄을 서게 된다.
 * 동시에 여러 편을 합성하면 서버가 버거우므로 풀은 작게 잡는다.
 */
@Configuration
public class PinlogAsyncConfig {

    @Bean
    public TaskExecutor pinlogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("pinlog-");
        executor.initialize();
        return executor;
    }
}
