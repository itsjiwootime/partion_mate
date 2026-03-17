package com.project.partition_mate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomAuthException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public CustomAuthException(String message, HttpStatus httpStatus) {
        this(message, httpStatus, "AUTH_ERROR");
    }

    public CustomAuthException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public static final CustomAuthException EMAIL_DUPLICATE =
            new CustomAuthException("Email already exists", HttpStatus.CONFLICT);

    public static final CustomAuthException USER_NOT_FOUND =
            new CustomAuthException("User not found", HttpStatus.NOT_FOUND);

    public static final CustomAuthException INVALID_PASSWORD =
            new CustomAuthException("Password error", HttpStatus.UNAUTHORIZED);

    public static final CustomAuthException INVALID_REFRESH_TOKEN =
            new CustomAuthException("세션이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");

}
