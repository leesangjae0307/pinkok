package com.example.pinkok_backend.pinlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * libheif 의 {@code heif-convert} 로 HEIC 를 JPEG 로 바꾼다.
 *
 * <p>배포 서버(Docker)에는 {@code libheif-examples} 패키지로 설치돼 있다. 로컬 윈도우처럼 도구가 없는 곳에서는
 * 비어 있는 값을 돌려주고, 업로드는 "HEIC 는 변환할 수 없다"는 안내로 거절된다.
 *
 * <p>사진 회전 정보(아이폰을 세로로 들고 찍은 것 등)는 heif-convert 가 반영해서, 바뀐 JPEG 는 바로 선 방향이 된다.
 */
@Component
public class HeifConvertHeicConverter implements HeicConverter {

    private static final Logger log = LoggerFactory.getLogger(HeifConvertHeicConverter.class);

    /** JPEG 품질 (0~100). 눈으로는 원본과 구분이 어려운 수준 */
    static final int JPEG_QUALITY = 92;
    private static final long TIMEOUT_SECONDS = 30;

    private final String heifConvertPath;

    public HeifConvertHeicConverter(@Value("${heif.convert-path:heif-convert}") String heifConvertPath) {
        this.heifConvertPath = heifConvertPath;
    }

    @Override
    public Optional<byte[]> toJpeg(byte[] heic) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("heic-");
            Path input = dir.resolve("in.heic");
            Path output = dir.resolve("out.jpg");
            Files.write(input, heic);

            if (!run(List.of(heifConvertPath, "-q", String.valueOf(JPEG_QUALITY), input.toString(), output.toString()))) {
                return Optional.empty();
            }
            // 사진 안에 이미지가 여러 장 들어 있으면(라이브 포토 등) out-1.jpg, out-2.jpg … 로 나온다. 첫 장을 쓴다
            Optional<Path> converted = Files.exists(output) ? Optional.of(output) : firstNumbered(dir);
            if (converted.isEmpty() || Files.size(converted.get()) == 0) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(converted.get()));

        } catch (IOException e) {
            log.debug("HEIC 를 변환하지 못했다", e);
            return Optional.empty();
        } finally {
            deleteQuietly(dir);
        }
    }

    private static Optional<Path> firstNumbered(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().matches("out-\\d+\\.jpg"))
                    .min(Comparator.comparing(p -> p.getFileName().toString()));
        }
    }

    private boolean run(List<String> command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            if (process.exitValue() != 0) {
                log.warn("heif-convert 실패: {}", output.trim());
                return false;
            }
            return true;
        } catch (IOException e) {
            log.debug("heif-convert 를 실행하지 못했다 (libheif 설치 여부와 heif.convert-path 설정을 확인)", e);
            return false;
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 임시 파일 정리 실패는 무시한다
                }
            });
        } catch (IOException ignored) {
            // 임시 폴더 정리 실패는 무시한다
        }
    }
}
