package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.FileUploadResponse;
import com.example.pinkok_backend.exception.FileUploadException;
import com.example.pinkok_backend.pinlog.VideoProbe;
import com.example.pinkok_backend.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class FileService {

    public static final int MAX_FILES_PER_REQUEST = 10;

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;  // 10MB
    private static final long MAX_VIDEO_SIZE = 20L * 1024 * 1024;  // 20MB (앱에서 2~3초로 찍는 영상 기준)

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov");

    private static final int THUMBNAIL_WIDTH = 300;
    private static final String THUMBNAIL_SUFFIX = "_thumb.jpg";
    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    /** diary_media.media_type 에 저장하는 값 */
    public static final String MEDIA_TYPE_PHOTO = "PHOTO";
    public static final String MEDIA_TYPE_VIDEO = "VIDEO";

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileStorage fileStorage;
    private final VideoProbe videoProbe;
    private final Executor uploadTaskExecutor;

    public FileService(FileStorage fileStorage, VideoProbe videoProbe,
                       @Qualifier("uploadTaskExecutor") Executor uploadTaskExecutor) {
        this.fileStorage = fileStorage;
        this.videoProbe = videoProbe;
        this.uploadTaskExecutor = uploadTaskExecutor;
    }

    /**
     * 파일 여러 개를 한 번에 올린다.
     * 하나라도 문제가 있으면 아무것도 저장하지 않고 거절한다. (일부만 저장되는 상황을 막기 위해)
     *
     * <p>파일마다 "원본 올리기"와 "썸네일 만들어 올리기"를 동시에 돌리고, 여러 파일도 동시에 처리한다.
     * 차례대로 하면 시간이 전부 더해지지만, 동시에 하면 가장 오래 걸리는 작업만큼만 걸린다.
     * 썸네일은 지금처럼 응답에 바로 들어간다 (앱이 올리자마자 썸네일을 보여줄 수 있도록 응답 전에 다 기다린다).
     */
    public List<FileUploadResponse> uploadAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileUploadException("올릴 파일이 없습니다.");
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new FileUploadException("한 번에 " + MAX_FILES_PER_REQUEST + "개까지 올릴 수 있습니다.");
        }

        long startedAt = System.nanoTime();

        // 1단계: 저장하기 전에 전부 검사한다
        List<CheckedFile> checked = new ArrayList<>();
        for (MultipartFile file : files) {
            checked.add(check(file));
        }

        // 2단계: 전부 통과했을 때만 저장한다 (여러 스레드가 저장하므로 저장한 경로는 동시에 써도 되는 목록에 모은다)
        Queue<String> savedPaths = new ConcurrentLinkedQueue<>();
        List<Path> tempFiles = new ArrayList<>();
        List<PendingUpload> pending = new ArrayList<>();
        try {
            for (CheckedFile file : checked) {
                pending.add(start(file, savedPaths, tempFiles));
            }
            // 하나가 실패해도 나머지가 다 끝나야 저장된 것을 깨끗이 지울 수 있으므로 전부 기다린다
            awaitAll(pending);

            List<FileUploadResponse> responses = new ArrayList<>();
            for (PendingUpload upload : pending) {
                responses.add(upload.toResponse());
            }
            log.info("파일 {}개 업로드 {}ms ({})", responses.size(), elapsedMs(startedAt),
                    pending.stream().map(PendingUpload::summary).collect(Collectors.joining(", ")));
            return responses;

        } catch (IOException | RuntimeException e) {
            // 일부만 저장된 채로 끝나지 않도록, 진행 중이던 작업이 다 끝난 뒤 저장한 파일을 지운다
            awaitQuietly(pending);
            deleteQuietly(new ArrayList<>(savedPaths));
            if (e instanceof FileUploadException uploadException) {
                throw uploadException;
            }
            throw new FileUploadException("파일 저장에 실패했습니다.", unwrap(e));
        } finally {
            tempFiles.forEach(this::deleteTempQuietly);
        }
    }

    private CheckedFile check(MultipartFile file) {
        String name = displayName(file);

        if (file.isEmpty()) {
            throw new FileUploadException(name + ": 파일이 비어 있습니다.");
        }

        String extension = getExtension(file.getOriginalFilename());

        if (IMAGE_EXTENSIONS.contains(extension)) {
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new FileUploadException(name + ": 사진은 10MB까지 올릴 수 있습니다.");
            }
            try {
                byte[] bytes = file.getBytes();
                // 확장자만 jpg 로 바꾼 가짜 파일을 걸러내기 위해 실제로 이미지로 읽어본다
                ImageInfo image = readImage(bytes);
                if (image == null) {
                    throw new FileUploadException(name + ": 이미지 파일을 읽을 수 없습니다.");
                }
                return new CheckedFile(file, extension, bytes, image);
            } catch (IOException e) {
                throw new FileUploadException(name + ": 이미지 파일을 읽을 수 없습니다.", e);
            }
        }

        if (VIDEO_EXTENSIONS.contains(extension)) {
            if (file.getSize() > MAX_VIDEO_SIZE) {
                throw new FileUploadException(name + ": 영상은 20MB까지 올릴 수 있습니다.");
            }
            return new CheckedFile(file, extension, null, null);
        }

        throw new FileUploadException(name + ": 지원하지 않는 파일 형식입니다. (사진: jpg, png / 영상: mp4, mov)");
    }

    /**
     * 사진의 가로·세로 크기를 읽고, 썸네일용으로 <b>작게 줄여서</b> 푼다.
     *
     * <p>예전에는 원본을 통째로 풀었다. 폰 사진(1200만 화소)이면 메모리에 48MB 가 올라가고 시간도 오래 걸린다.
     * 썸네일은 300px 이면 되므로 처음부터 몇 칸씩 건너뛰며 읽는다(subsampling). 크기는 파일 머리말에서 바로 읽힌다.
     * 썸네일 품질을 위해 목표의 2배(600px) 정도로 읽은 뒤 부드럽게 줄인다.
     */
    private ImageInfo readImage(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                ImageReadParam param = reader.getDefaultReadParam();
                int step = Math.max(1, width / (THUMBNAIL_WIDTH * 2));
                param.setSourceSubsampling(step, step, 0, 0);
                BufferedImage preview = reader.read(0, param);
                return preview == null ? null : new ImageInfo(width, height, preview);
            } finally {
                reader.dispose();
            }
        }
    }

    /** 파일 하나의 저장 작업을 시작한다 (원본과 썸네일을 각각 다른 스레드에서). */
    private PendingUpload start(CheckedFile file, Queue<String> savedPaths, List<Path> tempFiles) throws IOException {
        // 원래 파일명 대신 겹치지 않는 랜덤 이름을 쓰고, 연/월 폴더로 나눠 담는다
        String basePath = LocalDate.now().format(FOLDER_FORMAT) + "/" + UUID.randomUUID();
        String originalPath = basePath + "." + file.extension();
        String thumbnailPath = basePath + THUMBNAIL_SUFFIX;

        if (file.image() != null) {
            CompletableFuture<Timed<String>> original = async(() ->
                    storeTracked(new ByteArrayInputStream(file.bytes()), originalPath, savedPaths));
            CompletableFuture<Timed<Thumbnail>> thumbnail = async(() -> {
                String url = storeTracked(createThumbnail(file.image()), thumbnailPath, savedPaths);
                return new Thumbnail(url, null);
            });
            return new PendingUpload(file, original, thumbnail);
        }

        // 영상: FFmpeg 는 실제 파일이 있어야 해서 임시 파일로 한 번 쓰고, 원본 업로드도 그 파일에서 읽는다
        Path temp = Files.createTempFile("upload-", "." + file.extension());
        tempFiles.add(temp);
        file.source().transferTo(temp);

        CompletableFuture<Timed<String>> original = async(() -> {
            try (InputStream in = Files.newInputStream(temp)) {
                return storeTracked(in, originalPath, savedPaths);
            }
        });
        // FFmpeg 가 없거나 실패하면 썸네일·길이 없이 넘어간다 (업로드 자체는 되어야 한다)
        CompletableFuture<Timed<Thumbnail>> thumbnail = async(() -> {
            VideoProbe.Result probed = videoProbe.probe(temp);
            String url = null;
            if (probed.frame().isPresent()) {
                url = storeTracked(new ByteArrayInputStream(probed.frame().get()), thumbnailPath, savedPaths);
            }
            return new Thumbnail(url, probed.durationMs().orElse(null));
        });
        return new PendingUpload(file, original, thumbnail);
    }

    private String storeTracked(InputStream in, String path, Queue<String> savedPaths) throws IOException {
        String url = fileStorage.save(in, path);
        savedPaths.add(path);
        return url;
    }

    /** 작업을 업로드용 스레드 풀에 넘기고, 걸린 시간도 같이 잰다. */
    private <T> CompletableFuture<Timed<T>> async(IoTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            long startedAt = System.nanoTime();
            try {
                return new Timed<>(task.run(), elapsedMs(startedAt));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, uploadTaskExecutor);
    }

    private void awaitAll(List<PendingUpload> pending) {
        CompletableFuture.allOf(pending.stream()
                        .flatMap(upload -> java.util.stream.Stream.of(upload.original(), upload.thumbnail()))
                        .toArray(CompletableFuture[]::new))
                .join();
    }

    private void awaitQuietly(List<PendingUpload> pending) {
        try {
            awaitAll(pending);
        } catch (RuntimeException ignored) {
            // 실패한 작업이 있어도 "끝났는지"만 보면 된다
        }
    }

    private Throwable unwrap(Throwable e) {
        Throwable cause = e;
        while ((cause instanceof CompletionException || cause instanceof UncheckedIOException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private void deleteTempQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 정리 실패는 무시한다
        }
    }

    /**
     * 가로 300px 로 줄인 jpg 썸네일을 만든다. 원본이 더 작으면 크기를 그대로 둔다.
     * 비율은 원본 크기 기준으로 계산하고, 그리는 건 작게 풀어둔 이미지로 한다.
     */
    private InputStream createThumbnail(ImageInfo image) throws IOException {
        int width = Math.min(THUMBNAIL_WIDTH, image.width());
        int height = Math.max(1, (int) ((long) image.height() * width / image.width()));

        // jpg 는 투명 배경을 못 가지므로 흰 바탕 위에 그린다 (png 투명 영역이 까맣게 되는 것 방지)
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(image.preview(), 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "jpg", out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * 프로필 사진처럼 "사진 주소"를 받는 곳에서 쓴다.
     * 우리 업로드 API가 준 사진 원본 주소이고 파일이 실제로 있을 때만 통과한다.
     */
    public void validateImageUrl(String url) {
        String path = fileStorage.pathOf(url);
        if (path == null
                || path.endsWith(THUMBNAIL_SUFFIX)
                || !IMAGE_EXTENSIONS.contains(getExtension(path))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "업로드 API(POST /files)로 올린 사진 주소만 쓸 수 있습니다.");
        }
        if (!fileStorage.exists(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 사진입니다.");
        }
    }

    /**
     * 업로드된 이미지의 실제 파일 내용을 읽는다 (AI 분석기에 이미지를 그대로 넘길 때 등).
     * 우리 업로드 API로 올린 사진이 아니면(외부 주소 등) 거절한다.
     */
    public ImageBytes readImageBytes(String url) {
        String path = fileStorage.pathOf(url);
        String extension = path == null ? "" : getExtension(path);
        if (path == null || !IMAGE_EXTENSIONS.contains(extension) || !fileStorage.exists(path)) {
            throw new FileUploadException(url + ": 업로드 API(POST /files)로 올린 사진 주소만 쓸 수 있습니다.");
        }
        try {
            return new ImageBytes(fileStorage.read(path), mimeTypeOf(extension));
        } catch (IOException e) {
            throw new FileUploadException(url + ": 사진을 읽는 데 실패했습니다.", e);
        }
    }

    private String mimeTypeOf(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            default -> "image/jpeg"; // jpg, jpeg
        };
    }

    public record ImageBytes(byte[] bytes, String mimeType) {
    }

    /**
     * 더 이상 쓰지 않는 파일을 원본·썸네일 모두 지운다.
     * 파일 정리는 부가 작업이라, 실패해도 예외를 던지지 않는다.
     */
    public void deleteMediaQuietly(String url) {
        String path = fileStorage.pathOf(url);
        if (path == null) {
            return;
        }
        List<String> paths = new ArrayList<>();
        paths.add(path);
        int dot = path.lastIndexOf('.');
        if (dot > 0) {
            paths.add(path.substring(0, dot) + THUMBNAIL_SUFFIX);
        }
        deleteQuietly(paths);
    }

    private void deleteQuietly(List<String> paths) {
        for (String path : paths) {
            try {
                fileStorage.delete(path);
            } catch (IOException ignored) {
                // 정리 실패는 원래 에러를 가리지 않도록 무시한다
            }
        }
    }

    private String displayName(MultipartFile file) {
        String original = file.getOriginalFilename();
        return (original == null || original.isBlank()) ? "이름 없는 파일" : original;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /** 검사를 통과한 파일. 사진이면 이미 읽어둔 내용(bytes, image)을 저장할 때 다시 쓴다. */
    /**
     * 기록 첨부처럼 사진·영상을 모두 받는 곳에서 쓴다.
     * 우리 업로드 API가 준 원본 주소이고 파일이 실제로 있을 때만 통과하고, 종류(PHOTO/VIDEO)를 돌려준다.
     *
     * <p>종류를 앱이 보낸 값 대신 확장자로 우리가 직접 정하는 이유는,
     * 앱이 사진을 VIDEO 라고 보내는 식으로 틀리면 PinLog 합성 때 깨지기 때문이다.
     */
    public String resolveMediaType(String url) {
        String path = fileStorage.pathOf(url);
        if (path == null || path.endsWith(THUMBNAIL_SUFFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "업로드 API(POST /files)로 올린 파일 주소만 쓸 수 있습니다.");
        }

        String extension = getExtension(path);
        String mediaType;
        if (IMAGE_EXTENSIONS.contains(extension)) {
            mediaType = MEDIA_TYPE_PHOTO;
        } else if (VIDEO_EXTENSIONS.contains(extension)) {
            mediaType = MEDIA_TYPE_VIDEO;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "지원하지 않는 파일 형식입니다. (사진: jpg, png / 영상: mp4, mov)");
        }

        if (!fileStorage.exists(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 파일입니다.");
        }
        return mediaType;
    }

    /**
     * 업로드할 때 같이 만들어 둔 썸네일 주소를 돌려준다. 실제로 파일이 있을 때만 준다.
     *
     * <p>사진은 줄인 이미지, 영상은 첫 장면이다. 영상 썸네일은 FFmpeg 가 없으면 못 만들므로
     * 그 경우엔 null 이 된다 — 앱은 썸네일이 없을 수도 있다고 보고 그리면 된다.
     */
    public String thumbnailUrlOf(String url) {
        String path = fileStorage.pathOf(url);
        String extension = path == null ? "" : getExtension(path);
        if (path == null || !(IMAGE_EXTENSIONS.contains(extension) || VIDEO_EXTENSIONS.contains(extension))) {
            return null;
        }
        int dot = path.lastIndexOf('.');
        String thumbnailPath = path.substring(0, dot) + THUMBNAIL_SUFFIX;
        if (!fileStorage.exists(thumbnailPath)) {
            return null;
        }
        int urlDot = url.lastIndexOf('.');
        return url.substring(0, urlDot) + THUMBNAIL_SUFFIX;
    }

    /**
     * 예전 파일은 DB 저장이 끝난 뒤에 지운다.
     * 먼저 지웠다가 DB 저장이 실패하면, DB는 예전 주소를 가리키는데 파일은 없는 상태가 되기 때문이다.
     */
    public void deleteAfterCommit(String url) {
        if (url == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteMediaQuietly(url);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteMediaQuietly(url);
            }
        });
    }

    private record CheckedFile(MultipartFile source, String extension, byte[] bytes, ImageInfo image) {
    }

    /** 사진의 실제 크기와, 썸네일용으로 작게 풀어둔 이미지 */
    private record ImageInfo(int width, int height, BufferedImage preview) {
    }

    private record Thumbnail(String url, Integer durationMs) {
    }

    private record Timed<T>(T value, long elapsedMs) {
    }

    @FunctionalInterface
    private interface IoTask<T> {
        T run() throws IOException;
    }

    /** 파일 하나의 진행 중인 저장 작업 (원본 / 썸네일) */
    private record PendingUpload(CheckedFile file,
                                 CompletableFuture<Timed<String>> original,
                                 CompletableFuture<Timed<Thumbnail>> thumbnail) {

        FileUploadResponse toResponse() {
            String url = original.join().value();
            Thumbnail thumb = thumbnail.join().value();
            if (file.image() != null) {
                return new FileUploadResponse(url, thumb.url(), "IMAGE", file.source().getSize(),
                        file.image().width(), file.image().height(), null);
            }
            return new FileUploadResponse(url, thumb.url(), "VIDEO", file.source().getSize(),
                    null, null, thumb.durationMs());
        }

        /** 로그용: "영상 12.3MB 원본 2100ms·썸네일 900ms" */
        String summary() {
            return String.format(Locale.ROOT, "%s %.1fMB 원본 %dms·썸네일 %dms",
                    file.image() != null ? "사진" : "영상",
                    file.source().getSize() / 1024.0 / 1024.0,
                    original.join().elapsedMs(), thumbnail.join().elapsedMs());
        }
    }
}
