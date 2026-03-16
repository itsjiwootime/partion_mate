package com.project.partition_mate.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PartyDeadlineScheduler {

    private final PartyLifecycleService partyLifecycleService;

    public PartyDeadlineScheduler(PartyLifecycleService partyLifecycleService) {
        this.partyLifecycleService = partyLifecycleService;
    }

    @Scheduled(fixedDelayString = "${partition.party.deadline-check-delay-ms:30000}")
    public void closeExpiredParties() {
        partyLifecycleService.closeExpiredParties();
    }
}
