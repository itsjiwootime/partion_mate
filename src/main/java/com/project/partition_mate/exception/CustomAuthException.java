package com.project.partition_mate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomAuthException extends RuntimeException {

    private final HttpStatus httpStatus;

    public CustomAuthException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public static final CustomAuthException EMAIL_DUPLICATE =
            new CustomAuthException("Email already exists", HttpStatus.CONFLICT);

    public static final CustomAuthException USER_NOT_FOUND =
            new CustomAuthException("User not found", HttpStatus.NOT_FOUND);

    public static final CustomAuthException INVALID_PASSWORD =
            new CustomAuthException("Password error", HttpStatus.UNAUTHORIZED);




}
