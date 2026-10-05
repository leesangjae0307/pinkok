package com.example.pinkok_backend.pinlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 로 클립을 이어붙인다.
 *
 * <p><b>왜 두 단계인가.</b> 휴대폰마다 찍히는 영상의 크기·프레임레이트·소리 유무가 제각각이다.
 * 규격이 다른 영상을 그대로 이어붙이면 화면이 깨지거나 길이가 잘린다
 * (실제로 {@code -c copy} 로 붙였을 때 5초짜리가 4초로 잘리는 것을 확인했다). 그래서
 *
 * <ol>
 *   <li>클립마다 <b>같은 규격으로 다시 인코딩</b>한다 (720x1280 · 30fps · AAC 44.1kHz 스테레오)</li>
 *   <li>규격이 같아진 뒤에 <b>이어붙인다</b> — 이때는 다시 인코딩할 필요가 없어 빠르고 화질 손실도 없다</li>
 * </ol>
 *
 * <p>소리가 없는 클립(사진, 무음 영상)에는 무음 트랙을 만들어 붙인다. 소리 있는 클립과
 * 없는 클립이 섞이면 이어붙이기가 깨지기 때문이다.
 */
@Component
public class FfmpegVideoComposer implements VideoComposer {

    private static final Logger log = LoggerFactory.getLogger(FfmpegVideoComposer.class);

    private static final int FPS = 30;

    /** 클립 하나가 너무 길면 PinLog 전체가 늘어진다. */
    private static final double MAX_CLIP_SECONDS = 5.0;
    /** 사진 한 장을 몇 초짜리 장면으로 만들지. */
    private static final double PHOTO_SECONDS = 2.0;

    private final String ffmpegPath;
    private final String ffprobePath;
    private final long timeoutSeconds;

    /**
     * 완성 영상의 크기. 기본은 가로(16:9) — 브이로그는 가로로 보는 게 자연스럽다.
     * 세로(쇼츠 느낌)로 바꾸고 싶으면 설정만 720x1280 으로 돌리면 된다.
     */
    private final int width;
    private final int height;
    private final String scaleFilter;

    public FfmpegVideoComposer(@Value("${ffmpeg.path:ffmpeg}") String ffmpegPath,
                               @Value("${ffmpeg.probe-path:ffprobe}") String ffprobePath,
                               @Value("${ffmpeg.timeout-seconds:600}") long timeoutSeconds,
                               @Value("${pinlog.width:1280}") int width,
                               @Value("${pinlog.height:720}") int height) {
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
        this.timeoutSeconds = timeoutSeconds;
        this.width = width;
        this.height = height;

        // 비율은 그대로 두고 줄인 다음(decrease), 남는 자리는 가운데 정렬로 검은 띠를 채운다(pad).
        // 세로로 찍은 영상을 가로 화면에 넣으면 좌우에 검은 띠가 생긴다 — 잘라내는 것보다 안전하다.
        this.scaleFilter = "scale=" + width + ":" + height + ":force_original_aspect_ratio=decrease,"
                + "pad=" + width + ":" + height + ":(ow-iw)/2:(oh-ih)/2,"
                + "setsar=1,fps=" + FPS;
    }

    @Override
    public Result compose(List<Clip> clips, Path output, Path thumbnail) {
        if (clips.isEmpty()) {
            throw new VideoComposeException("이어붙일 클립이 없습니다.");
        }

        Path workDir = output.getParent();
        List<Path> parts = new ArrayList<>();
        for (int i = 0; i < clips.size(); i++) {
            Path part = workDir.resolve("part" + i + ".mp4");
            normalize(clips.get(i), part);
            parts.add(part);
        }

        concat(parts, workDir, output);
        extractThumbnail(output, thumbnail);

        int durationMs = (int) Math.round(probeDurationSeconds(output) * 1000);
        return new Result(durationMs, width + "x" + height);
    }

    /** 클립 하나를 공통 규격으로 다시 인코딩한다. */
    private void normalize(Clip clip, Path output) {
        double seconds = lengthOf(clip);

        List<String> command = new ArrayList<>(List.of(ffmpegPath, "-y"));
        if (clip.photo()) {
            // 사진 한 장을 지정한 시간만큼 반복해서 "정지 장면"으로 만든다
            command.addAll(List.of("-loop", "1", "-t", format(seconds), "-i", clip.file().toString()));
        } else {
            // -ss 를 -i 앞에 두면 그 지점까지 빨리 건너뛴다
            command.addAll(List.of("-ss", format(clip.startMs() / 1000.0),
                    "-t", format(seconds), "-i", clip.file().toString()));
        }

        boolean hasAudio = !clip.photo() && hasAudioStream(clip.file());
        if (!hasAudio) {
            // 소리가 없으면 무음 트랙을 만들어 붙인다 (소리 있는 클립과 규격을 맞추기 위해)
            command.addAll(List.of("-f", "lavfi", "-t", format(seconds),
                    "-i", "anullsrc=channel_layout=stereo:sample_rate=44100"));
        }

        command.addAll(List.of(
                "-map", "0:v:0",
                "-map", hasAudio ? "0:a:0" : "1:a:0",
                "-vf", scaleFilter,
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-ar", "44100", "-ac", "2", "-b:a", "128k",
                "-shortest",
                output.toString()));

        run(command, "클립을 변환하지 못했습니다");
    }

    private double lengthOf(Clip clip) {
        if (clip.photo()) {
            return PHOTO_SECONDS;
        }
        double start = clip.startMs() / 1000.0;
        double end = clip.endMs() == null ? probeDurationSeconds(clip.file()) : clip.endMs() / 1000.0;
        double seconds = Math.max(0.1, end - start);
        return Math.min(seconds, MAX_CLIP_SECONDS);
    }

    /**
     * 규격이 같아진 클립들을 이어붙인다. concat demuxer 는 "파일 목록"을 받으므로 목록 파일을 먼저 쓴다.
     * 여기서는 다시 인코딩하지 않는다(-c copy) — 이미 1단계에서 규격을 맞춰놨기 때문.
     */
    private void concat(List<Path> parts, Path workDir, Path output) {
        Path listFile = workDir.resolve("concat.txt");
        StringBuilder list = new StringBuilder();
        for (Path part : parts) {
            // 경로에 작은따옴표가 들어갈 일은 없지만, 형식상 감싸준다
            list.append("file '").append(part.toString().replace("\\", "/")).append("'\n");
        }
        try {
            Files.writeString(listFile, list.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new VideoComposeException("클립 목록 파일을 쓰지 못했습니다.", e);
        }

        run(List.of(ffmpegPath, "-y", "-f", "concat", "-safe", "0",
                "-i", listFile.toString(), "-c", "copy", output.toString()),
                "클립을 이어붙이지 못했습니다");
    }

    /** 완성된 영상의 첫 장면을 썸네일로 뽑는다. */
    private void extractThumbnail(Path video, Path thumbnail) {
        run(List.of(ffmpegPath, "-y", "-i", video.toString(),
                "-frames:v", "1", "-vf", "scale=480:-2", thumbnail.toString()),
                "썸네일을 만들지 못했습니다");
    }

    private boolean hasAudioStream(Path file) {
        String output = run(List.of(ffprobePath, "-v", "error",
                "-select_streams", "a", "-show_entries", "stream=index",
                "-of", "csv=p=0", file.toString()),
                "영상 정보를 읽지 못했습니다");
        return !output.isBlank();
    }

    private double probeDurationSeconds(Path file) {
        String output = run(List.of(ffprobePath, "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", file.toString()),
                "영상 길이를 읽지 못했습니다");
        try {
            return Double.parseDouble(output.trim());
        } catch (NumberFormatException e) {
            throw new VideoComposeException("영상 길이를 읽지 못했습니다: " + output.trim(), e);
        }
    }

    /**
     * 외부 프로그램을 실행하고 출력을 모은다.
     *
     * <p>출력을 읽어주지 않으면 버퍼가 차서 프로세스가 멈춘 채로 끝나지 않는다(교착).
     * 그래서 끝날 때까지 기다리기 전에 먼저 다 읽는다.
     */
    private String run(List<String> command, String failureMessage) {
        log.debug("실행: {}", String.join(" ", command));
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new VideoComposeException(failureMessage + " (시간 초과)");
            }
            if (process.exitValue() != 0) {
                throw new VideoComposeException(failureMessage + ": " + lastMeaningfulLine(output.toString()));
            }
            return output.toString();

        } catch (IOException e) {
            // ffmpeg 가 설치돼 있지 않거나 경로가 틀린 경우가 대부분이다
            throw new VideoComposeException(failureMessage + ": FFmpeg 를 실행할 수 없습니다. "
                    + "설치 여부와 ffmpeg.path 설정을 확인하세요.", e);
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new VideoComposeException(failureMessage + " (작업이 중단됨)", e);
        }
    }

    /** FFmpeg 는 진행 상황을 잔뜩 뱉으므로, 원인이 담긴 마지막 줄만 남긴다. */
    private String lastMeaningfulLine(String output) {
        String[] lines = output.strip().split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].strip();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return "알 수 없는 오류";
    }

    private String format(double seconds) {
        return String.format(java.util.Locale.ROOT, "%.3f", seconds);
    }
}
