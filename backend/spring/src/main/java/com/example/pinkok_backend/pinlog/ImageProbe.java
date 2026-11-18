package com.example.pinkok_backend.pinlog;

import java.util.Optional;

/**
 * 자바 기본 이미지 읽기(ImageIO)가 못 여는 사진(WebP 등)의 크기를 읽고 썸네일을 만든다.
 *
 * <p>인스타그램 같은 앱은 사진을 WebP 로 저장하는데, 자바는 JPEG·PNG 만 읽을 수 있다.
 * 원본은 변환하지 않고 그대로 저장하고(화질 그대로), 목록용 작은 썸네일만 FFmpeg 로 만든다.
 */
public interface ImageProbe {

    /** 가로·세로 크기. 이미지로 읽을 수 없으면 비어 있음 (이걸로 "진짜 이미지인지"도 확인한다) */
    Optional<Size> size(byte[] image, String extension);

    /** 가로 {@code maxWidth} 이하로 줄인 JPEG 썸네일. 원본이 더 작으면 크기 그대로. 실패하면 비어 있음 */
    Optional<byte[]> thumbnailJpeg(byte[] image, String extension, int maxWidth);

    record Size(int width, int height) {
    }
}
