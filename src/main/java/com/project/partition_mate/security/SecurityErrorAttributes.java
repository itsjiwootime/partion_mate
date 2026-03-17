package com.project.partition_mate.security;

public final class SecurityErrorAttributes {

    public static final String AUTH_ERROR_CODE = "auth.error.code";
    public static final String AUTH_ERROR_MESSAGE = "auth.error.message";

    public static final String CODE_AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String CODE_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String CODE_INVALID_TOKEN = "INVALID_TOKEN";
    public static final String CODE_ACCESS_DENIED = "ACCESS_DENIED";

    public static final String MESSAGE_AUTH_REQUIRED = "로그인이 필요합니다.";
    public static final String MESSAGE_TOKEN_EXPIRED = "로그인이 만료되었습니다. 다시 로그인해주세요.";
    public static final String MESSAGE_INVALID_TOKEN = "유효하지 않은 인증 정보입니다. 다시 로그인해주세요.";
    public static final String MESSAGE_ACCESS_DENIED = "접근 권한이 없습니다.";

    private SecurityErrorAttributes() {
    }
}
