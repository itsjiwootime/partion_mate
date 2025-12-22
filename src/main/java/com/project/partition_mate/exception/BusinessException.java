package com.project.partition_mate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static BusinessException insufficientQuantity(int remaining) {
        return new BusinessException("남은 수량(" + remaining + "개)이 부족하여 참여할 수 없습니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException notRecruiting() {
        return new BusinessException("모집 중인 파티가 아닙니다.", HttpStatus.CONFLICT);
    }
}
