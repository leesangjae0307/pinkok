package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.FileUploadResponse;
import com.example.pinkok_backend.exception.FileUploadException;
import com.example.pinkok_backend.storage.FileStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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

    private final FileStorage fileStorage;

    public FileService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * 파일 여러 개를 한 번에 올린다.
     * 하나라도 문제가 있으면 아무것도 저장하지 않고 거절한다. (일부만 저장되는 상황을 막기 위해)
     */
    public List<FileUploadResponse> uploadAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileUploadException("올릴 파일이 없습니다.");
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new FileUploadException("한 번에 " + MAX_FILES_PER_REQUEST + "개까지 올릴 수 있습니다.");
        }

        // 1단계: 저장하기 전에 전부 검사한다
        List<CheckedFile> checked = new ArrayList<>();
        for (MultipartFile file : files) {
            checked.add(check(file));
        }

        // 2단계: 전부 통과했을 때만 저장한다
        List<String> savedPaths = new ArrayList<>();
        try {
            List<FileUploadResponse> responses = new ArrayList<>();
            for (CheckedFile file : checked) {
                responses.add(save(file, savedPaths));
            }
            return responses;
        } catch (IOException e) {
            // 저장 도중 실패하면 앞에서 이미 저장한 파일도 지워서 반쪽짜리 업로드를 남기지 않는다
            deleteQuietly(savedPaths);
            throw new FileUploadException("파일 저장에 실패했습니다.", e);
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
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
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
            // 영상 썸네일·길이 확인은 FFmpeg 가 필요해서 PinLog 작업 때 추가한다
            return new CheckedFile(file, extension, null, null);
        }

        throw new FileUploadException(name + ": 지원하지 않는 파일 형식입니다. (사진: jpg, png / 영상: mp4, mov)");
    }

    private FileUploadResponse save(CheckedFile file, List<String> savedPaths) throws IOException {
        // 원래 파일명 대신 겹치지 않는 랜덤 이름을 쓰고, 연/월 폴더로 나눠 담는다
        String basePath = LocalDate.now().format(FOLDER_FORMAT) + "/" + UUID.randomUUID();
        String originalPath = basePath + "." + file.extension();

        if (file.image() != null) {
            String url = fileStorage.save(new ByteArrayInputStream(file.bytes()), originalPath);
            savedPaths.add(originalPath);

            String thumbnailPath = basePath + THUMBNAIL_SUFFIX;
            String thumbnailUrl = fileStorage.save(createThumbnail(file.image()), thumbnailPath);
            savedPaths.add(thumbnailPath);

            return new FileUploadResponse(url, thumbnailUrl, "IMAGE", file.source().getSize(),
                    file.image().getWidth(), file.image().getHeight());
        }

        try (InputStream in = file.source().getInputStream()) {
            String url = fileStorage.save(in, originalPath);
            savedPaths.add(originalPath);
            return new FileUploadResponse(url, null, "VIDEO", file.source().getSize(), null, null);
        }
    }

    /**
     * 가로 300px 로 줄인 jpg 썸네일을 만든다. 원본이 더 작으면 크기를 그대로 둔다.
     */
    private InputStream createThumbnail(BufferedImage original) throws IOException {
        int width = Math.min(THUMBNAIL_WIDTH, original.getWidth());
        int height = Math.max(1, original.getHeight() * width / original.getWidth());

        // jpg 는 투명 배경을 못 가지므로 흰 바탕 위에 그린다 (png 투명 영역이 까맣게 되는 것 방지)
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(original, 0, 0, width, height, null);
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
     * 더 이상 쓰지 않는 사진을 원본·썸네일 모두 지운다.
     * 파일 정리는 부가 작업이라, 실패해도 예외를 던지지 않는다.
     */
    public void deleteImageQuietly(String url) {
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
    private record CheckedFile(MultipartFile source, String extension, byte[] bytes, BufferedImage image) {
    }
}
