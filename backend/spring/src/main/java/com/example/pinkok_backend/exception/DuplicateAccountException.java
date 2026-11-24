package com.example.pinkok_backend.exception;

/**
 * 회원가입에서 이미 쓰고 있는 이메일·아이디일 때. 409 로 응답하며, 어느 항목인지 앱이 알 수 있게
 * {@code errorCode} 와 {@code errors} 를 함께 내려준다 (메시지 문구를 뒤져서 짐작하지 않아도 되게).
 *
 * <p>{@link IllegalArgumentException} 을 상속해서, 이 예외를 모르는 쪽에서는 지금까지처럼 409 + 메시지로 보인다.
 */
public class DuplicateAccountException extends IllegalArgumentException {

    public static final String EMAIL_DUPLICATED = "EMAIL_DUPLICATED";
    public static final String USERNAME_DUPLICATED = "USERNAME_DUPLICATED";

    private final String field;
    private final String errorCode;

    private DuplicateAccountException(String field, String errorCode, String message) {
        super(message);
        this.field = field;
        this.errorCode = errorCode;
    }

    public static DuplicateAccountException email() {
        return new DuplicateAccountException("email", EMAIL_DUPLICATED, "이미 존재하는 이메일입니다.");
    }

    public static DuplicateAccountException username() {
        return new DuplicateAccountException("username", USERNAME_DUPLICATED, "이미 존재하는 아이디입니다.");
    }

    /** 요청 본문의 필드 이름 (email / username). */
    public String getField() {
        return field;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
