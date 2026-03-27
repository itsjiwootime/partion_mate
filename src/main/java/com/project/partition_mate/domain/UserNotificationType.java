package com.project.partition_mate.domain;

public enum UserNotificationType {
    PARTY_JOIN_CONFIRMED,
    PICKUP_UPDATED,
    PARTY_UPDATED,
    PARTY_CLOSED;

    public boolean supportsWebPush() {
        return switch (this) {
            case PICKUP_UPDATED, PARTY_CLOSED -> true;
            case PARTY_JOIN_CONFIRMED, PARTY_UPDATED -> false;
        };
    }

    public boolean defaultWebPushEnabled() {
        return supportsWebPush();
    }

    public String getDisplayLabel() {
        return switch (this) {
            case PARTY_JOIN_CONFIRMED -> "참여 확정";
            case PICKUP_UPDATED -> "픽업 일정 확정";
            case PARTY_UPDATED -> "파티 조건 변경";
            case PARTY_CLOSED -> "파티 종료";
        };
    }

    public String getDescription() {
        return switch (this) {
            case PARTY_JOIN_CONFIRMED -> "내 참여 요청이 확정되면 앱 안에서 알려줍니다.";
            case PICKUP_UPDATED -> "픽업 장소와 시간이 확정되거나 변경되면 알려줍니다.";
            case PARTY_UPDATED -> "모집 수량, 마감 시간, 안내 문구 등 파티 조건이 바뀌면 알려줍니다.";
            case PARTY_CLOSED -> "호스트 취소 또는 마감 시간 도달로 파티가 종료되면 알려줍니다.";
        };
    }

    public String getDeepLinkTargetLabel() {
        return switch (this) {
            case PARTY_JOIN_CONFIRMED -> "채팅방";
            case PICKUP_UPDATED, PARTY_UPDATED -> "파티 상세";
            case PARTY_CLOSED -> "알림 내역";
        };
    }
}
