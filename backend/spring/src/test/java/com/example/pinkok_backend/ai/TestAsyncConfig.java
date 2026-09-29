package com.example.pinkok_backend.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * 테스트에서는 AI 처리를 "비동기"로 두면 assert 시점에 아직 안 끝났을 수 있어서,
 * 같은 스레드에서 즉시 실행하는 SyncTaskExecutor 로 바꿔치기한다.
 * (@Primary 라서 이름이 같은 진짜 aiTaskExecutor 빈보다 우선 주입된다.)
 */
@TestConfiguration
public class TestAsyncConfig {

    @Bean
    @Primary
    public TaskExecutor testAiTaskExecutor() {
        return new SyncTaskExecutor();
    }
}
