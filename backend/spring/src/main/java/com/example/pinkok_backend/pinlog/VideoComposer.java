package com.example.pinkok_backend.pinlog;

import java.nio.file.Path;
import java.util.List;

/**
 * 클립 여러 개를 영상 한 편으로 이어붙이는 일.
 *
 * <p>실제 구현은 {@link FfmpegVideoComposer}(FFmpeg 실행)이다. 인터페이스로 끊어둔 이유는
 * 두 가지다. 테스트에서 가짜 구현으로 바꿔 끼워 FFmpeg 없이 빠르게 돌릴 수 있고,
 * 나중에 합성을 서버 밖(클라우드 등)으로 옮겨도 이 위의 코드는 그대로 둘 수 있다.
 * ({@code FileStorage} 를 로컬/클라우드로 갈아 끼우는 것과 같은 방식)
 */
public interface VideoComposer {

    /**
     * 클립들을 받은 순서대로 이어붙여 {@code output} 에 mp4 로 쓰고, {@code thumbnail} 에 첫 장면을 저장한다.
     *
     * @throws VideoComposeException 합성에 실패했을 때 (원인 메시지 포함)
     */
    Result compose(List<Clip> clips, Path output, Path thumbnail);

    /**
     * 합성할 재료 하나.
     *
     * @param file    서버에 복사해둔 원본 파일
     * @param photo   사진이면 true (정지 장면으로 만든다), 영상이면 false
     * @param startMs 영상에서 잘라낼 시작 지점
     * @param endMs   잘라낼 끝 지점. 비어 있으면 끝까지(최대 길이 제한은 구현이 건다)
     */
    record Clip(Path file, boolean photo, int startMs, Integer endMs) {
    }

    /**
     * @param durationMs 합쳐진 영상의 총 길이
     * @param resolution 예: 720x1280
     */
    record Result(int durationMs, String resolution) {
    }
}
