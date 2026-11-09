package com.example.pinkok_backend.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 서버가 켜질 때, 지난번에 끝내지 못한 PinLog 를 정리한다 (AiRequestRecovery 와 같은 이유).
 * 합성 자체는 PinLog 전용 스레드 풀로 넘기므로 서버 시작을 붙잡지 않는다.
 * (서버를 한 대만 띄운다는 전제다)
 */
@Component
public class PinlogRecovery implements ApplicationRunner {

    /** 빈이 만들어지는 시점은 요청을 받기 전이라, 이후에 만든 새 PinLog 와 구분하는 기준이 된다. */
    private final LocalDateTime startedAt = LocalDateTime.now();

    private final PinlogService pinlogService;

    public PinlogRecovery(PinlogService pinlogService) {
        this.pinlogService = pinlogService;
    }

    @Override
    public void run(ApplicationArguments args) {
        pinlogService.recoverInterrupted(startedAt);
    }
}
