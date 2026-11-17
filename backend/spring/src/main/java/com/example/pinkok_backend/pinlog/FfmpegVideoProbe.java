package com.example.pinkok_backend.pinlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 로 영상의 썸네일 한 장면과 길이를 뽑는다.
 *
 * <p>업로드 도중에 불리므로 <b>실패해도 예외를 던지지 않는다.</b> FFmpeg 가 깔려 있지 않은 환경에서도
 * 영상 업로드 자체는 되어야 하기 때문이다 (그 경우 썸네일만 없는 상태가 된다).
 */
@Component
public class FfmpegVideoProbe implements VideoProbe {

    private static final Logger log = LoggerFactory.getLogger(FfmpegVideoProbe.class);

    /** 썸네일 가로 크기. 목록에 띄우는 용도라 작게. */
    private static final int THUMBNAIL_WIDTH = 480;
    private static final long TIMEOUT_SECONDS = 30;

    private final String ffmpegPath;
    private final String ffprobePath;

    public FfmpegVideoProbe(@Value("${ffmpeg.path:ffmpeg}") String ffmpegPath,
                            @Value("${ffmpeg.probe-path:ffprobe}") String ffprobePath) {
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
    }

    @Override
    public Optional<byte[]> firstFrame(Path video) {
        return frameAt(video, durationMs(video).orElse(null));
    }

    /**
     * 업로드할 때 쓰는 쪽. 길이를 먼저 구해서 썸네일 지점을 한 번에 고른다.
     * 예전에는 "1초 지점 시도 → 영상이 1초보다 짧으면 실패 → 0초로 다시"였어서 FFmpeg 가 두 번 돌 때가 있었다.
     */
    @Override
    public Result probe(Path video) {
        Optional<Integer> duration = durationMs(video);
        return new Result(frameAt(video, duration.orElse(null)), duration);
    }

    /**
     * 썸네일 한 장을 뽑는다. 맨 첫 장면(0초)은 까맣게 시작하는 폰 영상이 많아서 1초 지점을 쓰고,
     * 영상이 2초보다 짧으면 가운데를 쓴다. 길이를 모르면 1초 → 0초 순서로 시도한다.
     */
    private Optional<byte[]> frameAt(Path video, Integer durationMs) {
        Path thumbnail = null;
        try {
            thumbnail = Files.createTempFile("thumb-", ".jpg");
            boolean extracted;
            if (durationMs != null) {
                double seconds = Math.min(1.0, durationMs / 2000.0);
                extracted = extractFrame(video, thumbnail, String.format(java.util.Locale.ROOT, "%.3f", seconds))
                        || extractFrame(video, thumbnail, "0");
            } else {
                extracted = extractFrame(video, thumbnail, "1") || extractFrame(video, thumbnail, "0");
            }
            if (!extracted) {
                return Optional.empty();
            }
            byte[] bytes = Files.readAllBytes(thumbnail);
            return bytes.length == 0 ? Optional.empty() : Optional.of(bytes);

        } catch (IOException e) {
            log.debug("영상 썸네일을 만들지 못했다: {}", video, e);
            return Optional.empty();
        } finally {
            deleteQuietly(thumbnail);
        }
    }

    @Override
    public Optional<Integer> durationMs(Path video) {
        return run(List.of(ffprobePath, "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", video.toString()))
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .map(text -> {
                    try {
                        return (int) Math.round(Double.parseDouble(text) * 1000);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                });
    }

    private boolean extractFrame(Path video, Path thumbnail, String seconds) throws IOException {
        run(List.of(ffmpegPath, "-y", "-ss", seconds, "-i", video.toString(),
                "-frames:v", "1", "-vf", "scale=" + THUMBNAIL_WIDTH + ":-2",
                thumbnail.toString()));
        return Files.size(thumbnail) > 0;
    }

    /** 외부 프로그램 실행. 실패하면(미설치 포함) 비어 있는 값을 돌려준다. */
    private Optional<String> run(List<String> command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            return process.exitValue() == 0 ? Optional.of(output) : Optional.empty();

        } catch (IOException e) {
            log.debug("FFmpeg 를 실행하지 못했다 (설치 여부와 ffmpeg.path 설정을 확인)", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 정리 실패는 무시한다
        }
    }
}
