package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyCloseReason;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.domain.WaitingQueueEntry;
import com.project.partition_mate.domain.WaitingQueueStatus;
import com.project.partition_mate.dto.PartyRealtimeTrigger;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.WaitingQueueRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PartyLifecycleService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final PartyRealtimeService partyRealtimeService;
    private final StoreQueryCacheSupport storeQueryCacheSupport;
    private final ChatService chatService;
    private final Clock clock;

    public PartyLifecycleService(PartyRepository partyRepository,
                                 PartyMemberRepository partyMemberRepository,
                                 WaitingQueueRepository waitingQueueRepository,
                                 NotificationOutboxService notificationOutboxService,
                                 PartyRealtimeService partyRealtimeService,
                                 StoreQueryCacheSupport storeQueryCacheSupport,
                                 ChatService chatService,
                                 Clock clock) {
        this.partyRepository = partyRepository;
        this.partyMemberRepository = partyMemberRepository;
        this.waitingQueueRepository = waitingQueueRepository;
        this.notificationOutboxService = notificationOutboxService;
        this.partyRealtimeService = partyRealtimeService;
        this.storeQueryCacheSupport = storeQueryCacheSupport;
        this.chatService = chatService;
        this.clock = clock;
    }

    @Transactional
    public int closeExpiredParties() {
        return closeExpiredParties(LocalDateTime.now(clock));
    }

    @Transactional
    public int closeExpiredParties(LocalDateTime now) {
        List<Party> expiredParties = partyRepository.findAllByDeadlineLessThanEqualAndPartyStatusIn(
                now,
                List.of(PartyStatus.RECRUITING, PartyStatus.FULL)
        );

        int closedCount = 0;
        for (Party party : expiredParties) {
            closeExpiredParty(party, now);
            closedCount++;
        }

        return closedCount;
    }

    @Transactional
    public void closePartyByHost(Party party, User host, LocalDateTime now) {
        closeParty(
                party,
                now,
                PartyCloseReason.HOST_CANCELED,
                "호스트가 파티를 취소해 모집이 종료되었습니다.",
                host != null ? host.getId() : null
        );
    }

    private void closeExpiredParty(Party party, LocalDateTime now) {
        closeParty(
                party,
                now,
                PartyCloseReason.DEADLINE_EXPIRED,
                "마감 시간이 지나 모집이 종료되었습니다.",
                null
        );
    }

    private void closeParty(Party party,
                            LocalDateTime now,
                            PartyCloseReason closeReason,
                            String chatMessage,
                            Long excludedUserId) {
        List<PartyMember> joinedMembers = partyMemberRepository.findByParty(party);
        List<Long> joinedUserIds = joinedMembers.stream()
                .map(member -> member.getUser().getId())
                .filter(userId -> excludedUserId == null || !excludedUserId.equals(userId))
                .toList();

        List<WaitingQueueEntry> waitingEntries = waitingQueueRepository.findAllByPartyAndStatusOrderByQueuedAtAsc(
                party,
                WaitingQueueStatus.WAITING
        );
        List<Long> expiredWaitingUserIds = waitingEntries.stream()
                .map(waitingEntry -> waitingEntry.getUser().getId())
                .toList();

        waitingEntries.forEach(WaitingQueueEntry::expire);
        party.close(now, closeReason);

        notificationOutboxService.publishPartyClosed(party, joinedUserIds, expiredWaitingUserIds);
        chatService.appendSystemMessage(party, chatMessage);
        storeQueryCacheSupport.evictStoreQueries(party.getStore().getId());
        partyRealtimeService.publishPartyUpdatedAfterCommit(party, PartyRealtimeTrigger.PARTY_CLOSED);
    }
}
