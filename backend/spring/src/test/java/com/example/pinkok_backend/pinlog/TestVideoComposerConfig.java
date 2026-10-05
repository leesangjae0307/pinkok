package com.example.pinkok_backend.pinlog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 테스트에서 진짜 FFmpeg 대신 쓰는 가짜 합성기.
 *
 * <p>영상 합성은 한 번에 수십 초가 걸려서, 테스트마다 돌리면 테스트가 못 쓰게 느려진다.
 * 여기서는 "우리 코드가 클립을 올바른 순서로 넘기는가, 실패를 제대로 기록하는가" 같은
 * <b>흐름</b>만 확인하고, 진짜 FFmpeg 합성은 {@link FfmpegVideoComposerTest} 에서 따로 검증한다.
 */
@TestConfiguration
public class TestVideoComposerConfig {

    @Bean
    @Primary
    public FakeVideoComposer fakeVideoComposer() {
        return new FakeVideoComposer();
    }

    public static class FakeVideoComposer implements VideoComposer {

        /** 이 값을 채우면 합성이 실패한다 (실패 처리 테스트용). */
        private volatile String failureMessage;

        /** 마지막으로 합성 요청된 클립들 — 순서가 맞게 넘어왔는지 확인하는 데 쓴다. */
        private final List<Clip> lastClips = new ArrayList<>();

        public void reset() {
            failureMessage = null;
            lastClips.clear();
        }

        public void failNextWith(String message) {
            this.failureMessage = message;
        }

        public List<Clip> lastClips() {
            return List.copyOf(lastClips);
        }

        @Override
        public Result compose(List<Clip> clips, Path output, Path thumbnail) {
            synchronized (lastClips) {
                lastClips.clear();
                lastClips.addAll(clips);
            }
            if (failureMessage != null) {
                throw new VideoComposeException(failureMessage);
            }
            try {
                Files.writeString(output, "fake-mp4", StandardCharsets.UTF_8);
                Files.writeString(thumbnail, "fake-jpg", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new VideoComposeException("가짜 합성 결과를 쓰지 못했습니다.", e);
            }
            return new Result(clips.size() * 2000, "1280x720");
        }
    }
}
