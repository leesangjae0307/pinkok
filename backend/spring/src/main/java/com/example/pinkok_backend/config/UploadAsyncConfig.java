package com.example.pinkok_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 파일 업로드를 나눠서 동시에 처리하는 스레드 풀.
 *
 * <p>업로드 한 번에 "원본을 저장소에 올리기"와 "썸네일 만들어 올리기"가 서로 상관없이 일어나는데,
 * 차례대로 하면 두 시간이 그대로 더해진다. 둘을 동시에 돌리면 긴 쪽 시간만큼만 걸린다.
 * 사진 여러 장을 한 번에 올릴 때도 장마다 동시에 처리한다.
 *
 * <p>대부분 저장소(Cloud Storage)로 보내는 네트워크 대기라 CPU 가 1개여도 몇 개는 동시에 돌려도 된다.
 * 응답 전에 전부 기다리므로(사용자는 결과를 바로 받는다) AI·PinLog 처럼 뒤에서 도는 작업과는 성격이 다르다.
 */
@Configuration
public class UploadAsyncConfig {

    @Bean
    public TaskExecutor uploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("upload-");
        executor.initialize();
        return executor;
    }
}
