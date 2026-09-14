package com.example.pinkok_backend.dto;

/**
 * 업로드 결과. 앱은 이 주소들을 받아서 기록·프로필 등을 저장할 때 함께 보낸다.
 *
 * @param url          원본 파일 주소
 * @param thumbnailUrl 썸네일 주소 (사진만, 영상은 null)
 * @param mediaType    IMAGE / VIDEO
 * @param size         파일 크기(byte)
 * @param width        가로 픽셀 (사진만)
 * @param height       세로 픽셀 (사진만)
 */
public record FileUploadResponse(
        String url,
        String thumbnailUrl,
        String mediaType,
        long size,
        Integer width,
        Integer height
) {
}
