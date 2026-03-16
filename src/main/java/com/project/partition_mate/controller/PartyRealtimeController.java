package com.project.partition_mate.controller;

import com.project.partition_mate.service.PartyRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/party")
public class PartyRealtimeController {

    private final PartyRealtimeService partyRealtimeService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(required = false) Long storeId,
                                @RequestParam(required = false) Long partyId) {
        return partyRealtimeService.subscribe(storeId, partyId);
    }
}
