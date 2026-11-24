package com.example.pinkok_backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** 응답 시각에 Z 를 붙이고, 들어오는 시각은 시간대가 있든 없든 UTC 로 읽는지 */
class JacksonTimeConfigTest {

    @Test
    @DisplayName("내보낼 때 UTC 표시 Z 를 붙인다. 초가 0 이어도 초까지, 밀리초는 있을 때만")
    void format_appendsZ() {
        assertThat(JacksonTimeConfig.format(LocalDateTime.of(2026, 11, 21, 5, 30)))
                .isEqualTo("2026-11-21T05:30:00Z");
        assertThat(JacksonTimeConfig.format(LocalDateTime.of(2026, 11, 21, 5, 30, 7, 123_456_789)))
                .isEqualTo("2026-11-21T05:30:07.123Z");
    }

    @Test
    @DisplayName("읽을 때 Z·+09:00 은 UTC 로 바꾸고, 시간대가 없으면 UTC 로 본다")
    void parse_normalizesToUtc() {
        LocalDateTime utc = LocalDateTime.of(2026, 11, 21, 5, 30);
        assertThat(JacksonTimeConfig.parse("2026-11-21T05:30:00Z")).isEqualTo(utc);
        assertThat(JacksonTimeConfig.parse("2026-11-21T14:30:00+09:00")).isEqualTo(utc);
        assertThat(JacksonTimeConfig.parse("2026-11-21T05:30:00")).isEqualTo(utc);
    }
}
