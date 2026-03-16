package com.project.partition_mate.dto;

import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyMemberRole;
import com.project.partition_mate.domain.PaymentStatus;
import com.project.partition_mate.domain.TradeStatus;
import lombok.Getter;

@Getter
public class PartySettlementMemberResponse {

    private final Long memberId;
    private final Long userId;
    private final String username;
    private final PartyMemberRole role;
    private final Integer requestedQuantity;
    private final Integer expectedAmount;
    private final Integer actualAmount;
    private final PaymentStatus paymentStatus;
    private final String paymentStatusLabel;
    private final TradeStatus tradeStatus;
    private final String tradeStatusLabel;
    private final boolean pickupAcknowledged;
    private final boolean reviewEligible;
    private final boolean reviewWritten;

    private PartySettlementMemberResponse(Long memberId,
                                          Long userId,
                                          String username,
                                          PartyMemberRole role,
                                          Integer requestedQuantity,
                                          Integer expectedAmount,
                                          Integer actualAmount,
                                          PaymentStatus paymentStatus,
                                          TradeStatus tradeStatus,
                                          boolean pickupAcknowledged,
                                          boolean reviewEligible,
                                          boolean reviewWritten) {
        this.memberId = memberId;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.requestedQuantity = requestedQuantity;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
        this.paymentStatus = paymentStatus;
        this.paymentStatusLabel = paymentStatus != null ? paymentStatus.getLabel() : null;
        this.tradeStatus = tradeStatus;
        this.tradeStatusLabel = tradeStatus != null ? tradeStatus.getLabel() : null;
        this.pickupAcknowledged = pickupAcknowledged;
        this.reviewEligible = reviewEligible;
        this.reviewWritten = reviewWritten;
    }

    public static PartySettlementMemberResponse from(PartyMember partyMember,
                                                     Integer expectedAmount,
                                                     Integer actualAmount,
                                                     boolean reviewWritten) {
        return new PartySettlementMemberResponse(
                partyMember.getId(),
                partyMember.getUser().getId(),
                partyMember.getUser().getUsername(),
                partyMember.getRole(),
                partyMember.getRequestedQuantity(),
                partyMember.getExpectedAmount() != null ? partyMember.getExpectedAmount() : expectedAmount,
                partyMember.getActualAmount() != null ? partyMember.getActualAmount() : actualAmount,
                partyMember.getPaymentStatus(),
                partyMember.getTradeStatus(),
                partyMember.isPickupAcknowledged(),
                partyMember.isReviewEligible(),
                reviewWritten
        );
    }
}
