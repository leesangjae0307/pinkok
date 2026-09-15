package com.example.pinkok_backend.exception;

/**
 * 업로드할 수 없는 파일일 때 던진다. (형식이 틀림, 너무 큼, 깨진 파일 등)
 */
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
