package com.example.pinkok_backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * IP 별 요청 횟수 제한 (1분 고정 창). 로그인 전에 열려 있는 이메일·아이디 중복 확인 API 를
 * 계정 존재 여부를 긁어 가는 데 쓰지 못하게 하려는 것이다.
 *
 * <p>서버 한 대의 메모리 안에서만 센다. Cloud Run 이 인스턴스를 여러 대 띄우면 대수만큼 한도가 늘어나지만,
 * 이 용도(남용 억제)에는 충분하다고 보고 외부 저장소는 쓰지 않았다.
 */
@Component
public class IpRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;
    /** 창이 지난 항목을 한꺼번에 치우는 기준. 메모리가 끝없이 늘지 않게 한다. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(long startedAt, int count) {
    }

    private final int maxPerMinute;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public IpRateLimiter(@Value("${auth.check.rate-limit-per-minute:60}") int maxPerMinute) {
        this(maxPerMinute, System::currentTimeMillis);
    }

    IpRateLimiter(int maxPerMinute, LongSupplier clock) {
        this.maxPerMinute = maxPerMinute;
        this.clock = clock;
    }

    /** 이번 요청을 허용하면 true. 한도를 넘으면 false. */
    public boolean tryAcquire(String key) {
        long now = clock.getAsLong();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= WINDOW_MILLIS);
        }
        int[] count = new int[1];
        windows.compute(key, (k, window) -> {
            Window current = window == null || now - window.startedAt() >= WINDOW_MILLIS
                    ? new Window(now, 0)
                    : window;
            count[0] = current.count() + 1;
            return new Window(current.startedAt(), count[0]);
        });
        return count[0] <= maxPerMinute;
    }

    /** 한도를 넘으면 429. */
    public void requireAllowed(String key) {
        if (!tryAcquire(key)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}
