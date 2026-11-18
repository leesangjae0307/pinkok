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
 * FFmpeg 로 사진 크기를 읽고 썸네일을 만든다 (WebP 처럼 자바가 못 읽는 형식용).
 *
 * <p>업로드 도중에 불리므로 예외를 던지지 않고 실패하면 비워서 돌려준다. FFmpeg 는 실제 파일이 있어야 해서
 * 받은 바이트를 임시 파일로 잠깐 쓰고, 끝나면 지운다.
 */
@Component
public class FfmpegImageProbe implements ImageProbe {

    private static final Logger log = LoggerFactory.getLogger(FfmpegImageProbe.class);
    private static final long TIMEOUT_SECONDS = 20;

    private final String ffmpegPath;
    private final String ffprobePath;

    public FfmpegImageProbe(@Value("${ffmpeg.path:ffmpeg}") String ffmpegPath,
                            @Value("${ffmpeg.probe-path:ffprobe}") String ffprobePath) {
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
    }

    @Override
    public Optional<Size> size(byte[] image, String extension) {
        Path input = null;
        try {
            input = writeTemp(image, extension);
            return run(List.of(ffprobePath, "-v", "error", "-select_streams", "v:0",
                    "-show_entries", "stream=width,height", "-of", "csv=p=0:s=x", input.toString()))
                    .map(String::trim)
                    .flatMap(FfmpegImageProbe::parseSize);
        } catch (IOException e) {
            log.debug("사진 크기를 읽지 못했다", e);
            return Optional.empty();
        } finally {
            deleteQuietly(input);
        }
    }

    @Override
    public Optional<byte[]> thumbnailJpeg(byte[] image, String extension, int maxWidth) {
        Path input = null;
        Path output = null;
        try {
            input = writeTemp(image, extension);
            output = Files.createTempFile("thumb-", ".jpg");
            // 가로를 maxWidth 로 줄이되 원본이 더 작으면 그대로, 세로는 비율대로.
            // 투명 배경(WebP 알파)이 JPEG 로 가면서 검게 되지 않도록, 같은 크기의 흰 바탕을 깔고 그 위에 얹는다
            // (자바 썸네일과 같은 처리). drawbox 의 w·h 를 비워두면 화면 전체를 칠한다.
            String filter = "[0:v]scale='min(" + maxWidth + ",iw)':-2,format=rgba,split[a][b];"
                    + "[a]drawbox=c=white@1:t=fill[bg];"
                    + "[bg][b]overlay=format=auto,format=yuvj420p";
            Optional<String> result = run(List.of(ffmpegPath, "-y", "-i", input.toString(),
                    "-filter_complex", filter, "-frames:v", "1", "-q:v", "3", output.toString()));
            if (result.isEmpty() || Files.size(output) == 0) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(output));
        } catch (IOException e) {
            log.debug("사진 썸네일을 만들지 못했다", e);
            return Optional.empty();
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private static Optional<Size> parseSize(String text) {
        String[] parts = text.split("x");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            return width > 0 && height > 0 ? Optional.of(new Size(width, height)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Path writeTemp(byte[] image, String extension) throws IOException {
        Path path = Files.createTempFile("image-", "." + extension);
        Files.write(path, image);
        return path;
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
            if (process.exitValue() != 0) {
                log.debug("FFmpeg 실패: {}", output);
                return Optional.empty();
            }
            return Optional.of(output);
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

    private static void deleteQuietly(Path path) {
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
