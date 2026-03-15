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

    public static BusinessException alreadyJoined() {
        return new BusinessException("이미 참여한 파티입니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException alreadyWaiting() {
        return new BusinessException("이미 대기 중인 파티입니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException notJoinedOrWaiting() {
        return new BusinessException("참여 중이거나 대기 중인 내역이 없습니다.", HttpStatus.NOT_FOUND);
    }

    public static BusinessException hostCannotCancel() {
        return new BusinessException("호스트는 참여 취소 대신 파티 종료 흐름을 사용해야 합니다.", HttpStatus.CONFLICT);
    }
}
