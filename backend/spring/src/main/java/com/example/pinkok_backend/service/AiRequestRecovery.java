package com.example.pinkok_backend.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 서버가 켜질 때, 지난번에 끝내지 못한 AI 요청을 마무리한다.
 *
 * <p>요청을 처리하던 스레드는 서버가 꺼지면 사라지므로, 켜진 뒤에 남아 있는 PENDING·PROCESSING 은
 * 아무도 처리하지 않는 요청이다. 카카오맵을 부를 수 있어 서버 시작을 붙잡지 않도록 별도 스레드에서 돈다.
 * (서버를 한 대만 띄운다는 전제다. 여러 대면 다른 서버가 처리 중인 요청까지 건드린다)
 */
@Component
public class AiRequestRecovery implements ApplicationRunner {

    /** 빈이 만들어지는 시점은 요청을 받기 전이라, 이후에 들어온 새 요청과 구분하는 기준이 된다. */
    private final LocalDateTime startedAt = LocalDateTime.now();

    private final AiRequestService aiRequestService;
    private final TaskExecutor aiTaskExecutor;

    public AiRequestRecovery(AiRequestService aiRequestService, TaskExecutor aiTaskExecutor) {
        this.aiRequestService = aiRequestService;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @Override
    public void run(ApplicationArguments args) {
        aiTaskExecutor.execute(() -> aiRequestService.recoverInterrupted(startedAt));
    }
}
