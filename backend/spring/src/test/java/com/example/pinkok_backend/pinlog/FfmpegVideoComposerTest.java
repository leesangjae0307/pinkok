package com.example.pinkok_backend.pinlog;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진짜 FFmpeg 로 합쳐보는 테스트.
 *
 * <p>다른 테스트는 가짜 합성기를 써서 흐름만 보지만, 그것만으로는 "정작 영상이 안 만들어지는" 것을
 * 못 잡는다. 여기서는 <b>크기·프레임레이트·소리 유무가 모두 다른</b> 재료를 일부러 만들어서
 * 실제로 한 편으로 합쳐지는지 확인한다 (이게 PinLog에서 제일 깨지기 쉬운 부분이다).
 *
 * <p>FFmpeg 가 설치돼 있지 않은 환경에서는 자동으로 건너뛴다.
 */
class FfmpegVideoComposerTest {

    @TempDir
    Path workDir;

    private static boolean commandExists(String command) {
        try {
            Process process = new ProcessBuilder(command, "-version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private void run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        assertThat(process.waitFor()).isEqualTo(0);
    }

    @Test
    @DisplayName("크기·프레임레이트·소리가 제각각인 재료를 한 편으로 합친다")
    void composesClipsWithDifferentFormats() throws Exception {
        Assumptions.assumeTrue(commandExists("ffmpeg") && commandExists("ffprobe"),
                "FFmpeg 가 설치돼 있지 않아 건너뜁니다.");

        // 재료 1: 320x240 · 30fps · 소리 있음
        Path withSound = workDir.resolve("with-sound.mp4");
        run(List.of("ffmpeg", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=2:size=320x240:rate=30",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=2",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-shortest",
                withSound.toString()));

        // 재료 2: 640x360 · 24fps · 소리 없음 (일부러 다르게)
        Path silent = workDir.resolve("silent.mp4");
        run(List.of("ffmpeg", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=2:size=640x360:rate=24",
                "-an", "-c:v", "libx264", "-pix_fmt", "yuv420p",
                silent.toString()));

        // 재료 3: 사진
        Path photo = workDir.resolve("photo.jpg");
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 800, 600);
        g.dispose();
        ImageIO.write(image, "jpg", photo.toFile());

        Path output = workDir.resolve("pinlog.mp4");
        Path thumbnail = workDir.resolve("pinlog_thumb.jpg");

        VideoComposer composer = new FfmpegVideoComposer("ffmpeg", "ffprobe", 600, 1280, 720);
        VideoComposer.Result result = composer.compose(List.of(
                new VideoComposer.Clip(withSound, false, 0, null),
                new VideoComposer.Clip(silent, false, 0, null),
                new VideoComposer.Clip(photo, true, 0, null)), output, thumbnail);

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isPositive();
        assertThat(Files.exists(thumbnail)).isTrue();
        assertThat(result.resolution()).isEqualTo("1280x720");
        // 2초 + 2초 + 사진 2초 = 6초 안팎
        assertThat(result.durationMs()).isBetween(5000, 7000);
    }

    @Test
    @DisplayName("영상에서 첫 장면 썸네일과 길이를 뽑는다 - PinLog 클립 선택 화면에서 쓴다")
    void probesThumbnailAndDuration() throws Exception {
        Assumptions.assumeTrue(commandExists("ffmpeg") && commandExists("ffprobe"),
                "FFmpeg 가 설치돼 있지 않아 건너뜁니다.");

        Path video = workDir.resolve("clip.mp4");
        run(List.of("ffmpeg", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=3:size=640x480:rate=30",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", video.toString()));

        VideoProbe probe = new FfmpegVideoProbe("ffmpeg", "ffprobe");

        assertThat(probe.firstFrame(video)).isPresent();
        assertThat(probe.firstFrame(video).get().length).isPositive();
        assertThat(probe.durationMs(video)).hasValueSatisfying(ms -> assertThat(ms).isBetween(2500, 3500));
    }

    @Test
    @DisplayName("FFmpeg 가 없으면 썸네일 없이 넘어간다 (업로드 자체는 되어야 한다)")
    void probeWithoutFfmpeg_returnsEmpty() {
        VideoProbe probe = new FfmpegVideoProbe("ffmpeg-not-installed", "ffprobe-not-installed");

        assertThat(probe.firstFrame(workDir.resolve("none.mp4"))).isEmpty();
        assertThat(probe.durationMs(workDir.resolve("none.mp4"))).isEmpty();
    }

    @Test
    @DisplayName("FFmpeg 실행 파일이 없으면 무슨 일인지 알 수 있는 메시지를 남긴다")
    void missingFfmpeg_givesClearMessage() {
        VideoComposer composer = new FfmpegVideoComposer("ffmpeg-not-installed", "ffprobe-not-installed", 10, 1280, 720);

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(VideoComposeException.class,
                        () -> composer.compose(
                                List.of(new VideoComposer.Clip(workDir.resolve("none.jpg"), true, 0, null)),
                                workDir.resolve("out.mp4"), workDir.resolve("thumb.jpg")))
                .getMessage())
                .contains("FFmpeg 를 실행할 수 없습니다");
    }
}
