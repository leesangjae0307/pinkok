package com.example.pinkok_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 파일 업로드 실패를 공통 에러 형식({timestamp, status, message})으로 돌려준다.
 * 형식은 backend/docs/에러-응답-형식.md 참고.
 */
@RestControllerAdvice
public class FileExceptionHandler {

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<Map<String, Object>> handleFileUpload(FileUploadException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 파일 하나가 20MB, 또는 합계가 100MB 를 넘으면 컨트롤러에 닿기 전에 여기로 온다
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSize(MaxUploadSizeExceededException e) {
        return error(HttpStatus.CONTENT_TOO_LARGE,
                "파일이 너무 큽니다. (사진 10MB, 영상 20MB, 한 번에 합계 100MB까지)");
    }

    // "file" 이라는 이름의 파일 칸이 없을 때 (이름을 틀렸거나, 파일 대신 글자를 보냈을 때)
    // 지금은 multipart 를 받는 곳이 업로드뿐이라 여기서 처리한다
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(MissingServletRequestPartException e) {
        return error(HttpStatus.BAD_REQUEST,
                "'" + e.getRequestPartName() + "' 이름으로 파일을 담아 보내주세요.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
