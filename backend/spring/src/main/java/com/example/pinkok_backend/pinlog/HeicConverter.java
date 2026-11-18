package com.example.pinkok_backend.pinlog;

import java.util.Optional;

/**
 * HEIC(아이폰 기본 사진 형식) 사진을 JPEG 로 바꾼다.
 *
 * <p>WebP 와 달리 HEIC 는 원본 그대로 둘 수 없다. 안드로이드 앱에서 안 보일 수 있고, 서버의 FFmpeg(PinLog 합성)도
 * HEIC 를 읽지 못한다. 그래서 업로드할 때 고화질 JPEG 로 바꿔서 저장하고, 그다음은 일반 JPEG 와 똑같이 처리한다.
 *
 * <p>아이폰 사진은 대부분 앱이 고를 때 이미 JPEG 로 바꿔서 보내고(이미지 선택기의 compatible 모드),
 * 이건 그래도 HEIC 가 들어왔을 때를 위한 안전망이다.
 */
public interface HeicConverter {

    /** 변환된 JPEG 바이트. 변환 도구가 없거나 실패하면 비어 있음 */
    Optional<byte[]> toJpeg(byte[] heic);
}
