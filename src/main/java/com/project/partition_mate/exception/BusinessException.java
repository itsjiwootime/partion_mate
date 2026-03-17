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

    public static BusinessException deadlineExpired() {
        return new BusinessException("마감 시간이 지나 종료된 파티입니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException partyClosed() {
        return new BusinessException("이미 종료된 파티입니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException onlyHostCanUpdateParty() {
        return new BusinessException("파티 수정은 호스트만 처리할 수 있습니다.", HttpStatus.FORBIDDEN);
    }

    public static BusinessException partyEditRestrictedAfterSettlementOrPickup() {
        return new BusinessException("정산 또는 픽업 일정이 진행된 뒤에는 안내 문구와 오픈채팅 링크만 수정할 수 있습니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException onlyHostCanManageSettlement() {
        return new BusinessException("정산 확정은 호스트만 처리할 수 있습니다.", HttpStatus.FORBIDDEN);
    }

    public static BusinessException onlyHostCanManagePickup() {
        return new BusinessException("픽업 일정 확정은 호스트만 처리할 수 있습니다.", HttpStatus.FORBIDDEN);
    }

    public static BusinessException onlyHostCanManageChatNotice() {
        return new BusinessException("호스트만 채팅 공지를 고정할 수 있습니다.", HttpStatus.FORBIDDEN);
    }

    public static BusinessException onlyHostCanManageTradeStatus() {
        return new BusinessException("거래 완료와 노쇼 처리는 호스트만 할 수 있습니다.", HttpStatus.FORBIDDEN);
    }

    public static BusinessException onlyParticipantCanMarkPaid() {
        return new BusinessException("참여자 본인만 송금 완료를 표시할 수 있습니다.", HttpStatus.FORBIDDEN);
    }

    public static BusinessException onlyParticipantCanAcknowledgePickup() {
        return new BusinessException("참여자만 픽업 일정을 확인할 수 있습니다.", HttpStatus.FORBIDDEN);
    }

    public static BusinessException settlementNotConfirmed() {
        return new BusinessException("아직 정산이 확정되지 않았습니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException pickupNotScheduled() {
        return new BusinessException("아직 픽업 일정이 확정되지 않았습니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException invalidPaymentStatusTransition() {
        return new BusinessException("허용되지 않는 송금 상태 변경입니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException invalidTradeStatusTransition() {
        return new BusinessException("허용되지 않는 거래 상태 변경입니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException hostMemberCannotBeManaged() {
        return new BusinessException("호스트 계정에는 참여자용 거래 상태를 적용할 수 없습니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException reviewNotEligible() {
        return new BusinessException("거래 완료된 상대에게만 후기를 작성할 수 있습니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException reviewDuplicate() {
        return new BusinessException("같은 파티에서 이미 작성한 후기입니다.", HttpStatus.CONFLICT);
    }

    public static BusinessException invalidReviewTarget() {
        return new BusinessException("후기 대상 사용자가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
