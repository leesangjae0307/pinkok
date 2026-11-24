package com.example.pinkok_backend.auth;

import com.example.pinkok_backend.security.IpRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpRateLimiterTest {

    @Test
    @DisplayName("한도까지는 허용하고 넘으면 막으며, 1분이 지나면 다시 센다")
    void limitsWithinAWindowAndResetsAfter() throws Exception {
        AtomicLong now = new AtomicLong(1_000);
        IpRateLimiter limiter = newLimiter(2, now);

        assertTrue(limiter.tryAcquire("1.1.1.1"));
        assertTrue(limiter.tryAcquire("1.1.1.1"));
        assertFalse(limiter.tryAcquire("1.1.1.1"));
        assertTrue(limiter.tryAcquire("2.2.2.2"), "다른 IP 는 따로 센다");

        now.addAndGet(59_999);
        assertFalse(limiter.tryAcquire("1.1.1.1"), "아직 같은 창 안");

        now.addAndGet(1);
        assertTrue(limiter.tryAcquire("1.1.1.1"), "1분이 지나 새 창");
    }

    /** 시계를 바꿔 끼울 수 있는 패키지 전용 생성자라, 테스트에서는 리플렉션으로 만든다. */
    private static IpRateLimiter newLimiter(int max, AtomicLong clock) throws Exception {
        var constructor = IpRateLimiter.class.getDeclaredConstructor(int.class, java.util.function.LongSupplier.class);
        constructor.setAccessible(true);
        return constructor.newInstance(max, (java.util.function.LongSupplier) clock::get);
    }
}
