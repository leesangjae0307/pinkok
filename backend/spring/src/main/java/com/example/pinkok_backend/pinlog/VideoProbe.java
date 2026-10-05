package com.example.pinkok_backend.pinlog;

import java.util.Optional;

/**
 * 영상에서 첫 장면(썸네일)과 길이를 뽑아낸다.
 *
 * <p>사진은 업로드할 때 바로 썸네일을 만들 수 있지만(ImageIO), 영상은 FFmpeg 가 있어야 한다.
 * PinLog 에서 "내 영상 / 팀원 영상"을 고르는 화면은 영상이 어떤 장면인지 보여줘야 하므로,
 * 업로드 시점에 첫 장면을 뽑아 썸네일로 저장한다.
 *
 * <p>FFmpeg 가 없는 환경에서도 업로드 자체는 되어야 하므로, 실패하면 예외를 던지지 않고 비워서 돌려준다.
 */
public interface VideoProbe {

    /**
     * @param video 서버에 저장된 영상 파일의 경로(저장소 기준이 아니라 실제 파일 경로)
     * @return 첫 장면 jpg 바이트. 뽑지 못하면 비어 있음
     */
    Optional<byte[]> firstFrame(java.nio.file.Path video);

    /** 영상 길이(ms). 알 수 없으면 비어 있음. */
    Optional<Integer> durationMs(java.nio.file.Path video);
}
