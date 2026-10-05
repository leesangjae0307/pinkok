package com.example.pinkok_backend.pinlog;

/** 영상 합성 실패. 사용자에게 보여줄 수 있도록 원인을 메시지에 담는다. */
public class VideoComposeException extends RuntimeException {

    public VideoComposeException(String message) {
        super(message);
    }

    public VideoComposeException(String message, Throwable cause) {
        super(message, cause);
    }
}
