package com.example.pinkok_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 폰 푸시 발송용 스레드 풀.
 *
 * <p>FCM 호출은 외부 네트워크라 느릴 수 있다. 초대 버튼을 누른 사람이 그걸 기다리지 않도록
 * 따로 돌린다. 영상 합성·AI 풀과 섞지 않아서, 그쪽이 바빠도 알림은 바로 나간다.
 */
@Configuration
public class PushAsyncConfig {

    @Bean
    public TaskExecutor pushTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("push-");
        executor.initialize();
        return executor;
    }
}
