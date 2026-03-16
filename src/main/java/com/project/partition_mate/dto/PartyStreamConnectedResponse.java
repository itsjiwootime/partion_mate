package com.project.partition_mate.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PartyStreamConnectedResponse {

    private final String streamId;
    private final LocalDateTime connectedAt;
    private final long reconnectDelayMs;

    private PartyStreamConnectedResponse(String streamId, LocalDateTime connectedAt, long reconnectDelayMs) {
        this.streamId = streamId;
        this.connectedAt = connectedAt;
        this.reconnectDelayMs = reconnectDelayMs;
    }

    public static PartyStreamConnectedResponse of(String streamId, LocalDateTime connectedAt, long reconnectDelayMs) {
        return new PartyStreamConnectedResponse(streamId, connectedAt, reconnectDelayMs);
    }
}
