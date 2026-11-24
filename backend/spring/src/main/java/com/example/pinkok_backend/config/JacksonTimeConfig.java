package com.example.pinkok_backend.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 응답에 나가는 시각(createdAt 등)에 시간대 표시(Z)를 붙인다.
 *
 * <p>서버는 시각을 시간대 없는 {@link LocalDateTime} 으로 다루고, 기본 시간대를 UTC 로 고정해 두었다
 * ({@code PinkokBackendApplication}). 그런데 그대로 내보내면 {@code 2026-11-21T05:30:00} 처럼 시간대가 빠져서,
 * 자바스크립트는 이를 기기 시간대(KST)로 읽는다. 그러면 방금 온 알림이 "9시간 전" 으로 보인다.
 * 그래서 내보낼 때 {@code 2026-11-21T05:30:00Z} 로 UTC 임을 밝힌다.
 *
 * <p>DTO 마다 붙이지 않고 여기서 한 번에 바꾼다. 읽을 때는 Z·+09:00 이 붙어 와도 UTC 로 바꿔 받고,
 * 시간대가 없으면 예전처럼 UTC 로 본다.
 */
@Configuration
public class JacksonTimeConfig {

    private static final DateTimeFormatter SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    JsonMapperBuilderCustomizer utcLocalDateTime() {
        SimpleModule module = new SimpleModule("UtcLocalDateTime");
        module.addSerializer(LocalDateTime.class, new UtcSerializer());
        module.addDeserializer(LocalDateTime.class, new UtcDeserializer());
        return builder -> builder.addModule(module);
    }

    /** 2026-11-21T05:30:00Z. 밀리초가 있으면 .123 까지 붙인다 (그 아래는 앱에서 쓸 일이 없다). */
    static String format(LocalDateTime value) {
        String base = value.format(SECONDS);
        int millis = value.getNano() / 1_000_000;
        return millis == 0 ? base + "Z" : base + String.format(".%03dZ", millis);
    }

    static LocalDateTime parse(String text) {
        String value = text.trim();
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException noOffset) {
            return LocalDateTime.parse(value);
        }
    }

    static class UtcSerializer extends ValueSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(format(value));
        }
    }

    static class UtcDeserializer extends ValueDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
            String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                return parse(text);
            } catch (DateTimeParseException e) {
                return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, text,
                        "날짜·시각 형식이 아닙니다 (예: 2026-11-21T05:30:00Z)");
            }
        }
    }
}
