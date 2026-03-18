package com.project.partition_mate.domain;

public enum UserNotificationType {
    PARTY_JOIN_CONFIRMED,
    WAITING_PROMOTED,
    PICKUP_UPDATED,
    PARTY_UPDATED,
    PARTY_CLOSED,
    WAITING_EXPIRED;

    public boolean supportsWebPush() {
        return switch (this) {
            case WAITING_PROMOTED, PICKUP_UPDATED, PARTY_CLOSED, WAITING_EXPIRED -> true;
            case PARTY_JOIN_CONFIRMED, PARTY_UPDATED -> false;
        };
    }

    public boolean defaultWebPushEnabled() {
        return supportsWebPush();
    }

    public String getDisplayLabel() {
        return switch (this) {
            case PARTY_JOIN_CONFIRMED -> "참여 확정";
            case WAITING_PROMOTED -> "대기열 승격";
            case PICKUP_UPDATED -> "픽업 일정 확정";
            case PARTY_UPDATED -> "파티 조건 변경";
            case PARTY_CLOSED -> "파티 종료";
            case WAITING_EXPIRED -> "대기열 종료";
        };
    }

    public String getDescription() {
        return switch (this) {
            case PARTY_JOIN_CONFIRMED -> "내 참여 요청이 확정되면 앱 안에서 알려줍니다.";
            case WAITING_PROMOTED -> "대기열에서 참여로 승격되면 알려줍니다.";
            case PICKUP_UPDATED -> "픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.";
            case PARTY_UPDATED -> "모집 수량, 마감 시간, 안내 문구 등 파티 조건이 바뀌면 알려줍니다.";
            case PARTY_CLOSED -> "호스트 취소 또는 마감 시간 도달로 파티가 종료되면 알려줍니다.";
            case WAITING_EXPIRED -> "대기 중인 파티가 종료되어 더 이상 승격되지 않을 때 알려줍니다.";
        };
    }

    public String getDeepLinkTargetLabel() {
        return switch (this) {
            case PARTY_JOIN_CONFIRMED, WAITING_PROMOTED -> "채팅방";
            case PICKUP_UPDATED, PARTY_UPDATED -> "파티 상세";
            case PARTY_CLOSED, WAITING_EXPIRED -> "알림 내역";
        };
    }
}
