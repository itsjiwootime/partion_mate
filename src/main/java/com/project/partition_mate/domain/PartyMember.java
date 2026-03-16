package com.project.partition_mate.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "partymember",
        uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private PartyMemberRole role;

    @Column(nullable = false)
    private Integer requestedQuantity;

    private Integer expectedAmount;

    private Integer actualAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    private LocalDateTime pickupAcknowledgedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus tradeStatus;

    @Builder
    public PartyMember(Party party, User user,PartyMemberRole role, Integer requestedQuantity) {

        this.party = Objects.requireNonNull(party,"파티를 선택해주세요");
        this.user = Objects.requireNonNull(user,"사용자는 필수입니다.");
        this.role = Objects.requireNonNull(role, "롤을 정해주세요");
        validateQuantity(requestedQuantity);
        this.requestedQuantity = requestedQuantity;
        this.paymentStatus = role == PartyMemberRole.LEADER ? PaymentStatus.NOT_REQUIRED : PaymentStatus.PENDING;
        this.tradeStatus = TradeStatus.PENDING;

    }

    public static PartyMember joinAsHost(Party party, User user, Integer requestedQuantity) {
        validateQuantity(requestedQuantity);
        return new PartyMember(party, user, PartyMemberRole.LEADER, requestedQuantity);
    }

    public static PartyMember joinAsMember(Party party, User user, Integer requestedQuantity) {
        validateMemberQuantity(requestedQuantity);
        return new PartyMember(party, user, PartyMemberRole.MEMBER, requestedQuantity);
    }

    public boolean isHost() {
        return this.role == PartyMemberRole.LEADER;
    }

    public boolean isMember() {
        return this.role == PartyMemberRole.MEMBER;
    }

    public void applySettlement(Integer expectedAmount, Integer actualAmount) {
        validateAmount(expectedAmount, "예상 정산 금액은 0 이상이어야 합니다.");
        validateAmount(actualAmount, "확정 정산 금액은 0 이상이어야 합니다.");
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;

        if (isHost()) {
            this.paymentStatus = PaymentStatus.NOT_REQUIRED;
            return;
        }

        if (this.paymentStatus != PaymentStatus.REFUNDED) {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }

    public void markPaid() {
        validateSettlementConfirmed();
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("송금 완료로 변경할 수 없는 상태입니다.");
        }
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void confirmPayment() {
        validateSettlementConfirmed();
        if (this.paymentStatus != PaymentStatus.PAID) {
            throw new IllegalArgumentException("호스트는 송금 완료 상태만 확인할 수 있습니다.");
        }
        this.paymentStatus = PaymentStatus.CONFIRMED;
    }

    public void refundPayment() {
        validateSettlementConfirmed();
        if (this.paymentStatus != PaymentStatus.PAID && this.paymentStatus != PaymentStatus.CONFIRMED) {
            throw new IllegalArgumentException("환불 처리할 수 없는 상태입니다.");
        }
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    public void acknowledgePickup(LocalDateTime acknowledgedAt) {
        this.pickupAcknowledgedAt = Objects.requireNonNull(acknowledgedAt, "픽업 확인 시각은 필수입니다.");
    }

    public void completeTrade() {
        this.tradeStatus = TradeStatus.COMPLETED;
    }

    public void markNoShow() {
        this.tradeStatus = TradeStatus.NO_SHOW;
    }

    public boolean isReviewEligible() {
        return isMember() && this.tradeStatus == TradeStatus.COMPLETED;
    }

    public boolean isPickupAcknowledged() {
        return this.pickupAcknowledgedAt != null;
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("요청 수량은 필수입니다.");
        }

        if (quantity < 0) {
            throw new IllegalArgumentException("요청 수량은 0 이상이어야 합니다.");
        }
    }

    private static void validateMemberQuantity(Integer quantity) {
        validateQuantity(quantity);

        if (quantity == 0) {
            throw new IllegalArgumentException("참여 요청 수량은 1 이상이어야 합니다.");
        }
    }

    private void validateSettlementConfirmed() {
        if (this.actualAmount == null) {
            throw new IllegalArgumentException("정산이 아직 확정되지 않았습니다.");
        }
    }

    private void validateAmount(Integer amount, String message) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
