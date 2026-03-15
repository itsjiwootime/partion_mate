package com.project.partition_mate.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "party")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column
    private String openChatUrl;

    @Enumerated(EnumType.STRING)
    private PartyStatus partyStatus;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
    private List<PartyMember> members =  new ArrayList<>();

    public Party(String title,
                 String productName,
                 Integer totalPrice,
                 Store store,
                 Integer totalQuantity,
                 String openChatUrl) {

        this.title = Objects.requireNonNull(title, "파티 제목은 필수입니다.");
        this.productName = Objects.requireNonNull(productName, "제품명은 필수입니다.");
        this.totalPrice = Objects.requireNonNull(totalPrice, "총 가격은 필수입니다.");
        this.totalQuantity = Objects.requireNonNull(totalQuantity, "총 수량은 필수입니다.");
        this.store = Objects.requireNonNull(store, "지점 정보는 필수입니다.");
        this.openChatUrl = openChatUrl;

        validateTotalPrice(totalPrice);
        validateTotalQuantity(totalQuantity);

        this.partyStatus = PartyStatus.RECRUITING;
    }

    public void acceptMember(PartyMember member) {
        Objects.requireNonNull(member, "파티 멤버는 필수입니다.");
        validateAcceptable(member);

        this.members.add(member);
        refreshStatusByQuantity();
    }

    public boolean isRecruiting() {
        return this.partyStatus == PartyStatus.RECRUITING;
    }

    public void removeMember(PartyMember member) {
        Objects.requireNonNull(member, "파티 멤버는 필수입니다.");
        this.members.removeIf(existingMember -> Objects.equals(existingMember.getId(), member.getId()));
        refreshStatusByQuantity();
    }

    public void refreshStatusByQuantity() {
        if (getRequestedQuantity() >= this.totalQuantity) {
            this.partyStatus = PartyStatus.FULL;
            return;
        }

        this.partyStatus = PartyStatus.RECRUITING;
    }

    public int getRemainingQuantity() {
        return Math.max(this.totalQuantity - getRequestedQuantity(), 0);
    }

    public int getRequestedQuantity() {
        return members.stream()
                .mapToInt(PartyMember::getRequestedQuantity)
                .sum();
    }

    private void validateAcceptable(PartyMember member) {
        if (!isRecruiting()) {
            throw com.project.partition_mate.exception.BusinessException.notRecruiting();
        }

        int remainingQuantity = getRemainingQuantity();
        int newMemberRequested = member.getRequestedQuantity();

        if (newMemberRequested > remainingQuantity) {
            throw com.project.partition_mate.exception.BusinessException.insufficientQuantity(remainingQuantity);
        }
    }

    private void validateTotalPrice(Integer totalPrice) {
        if (totalPrice < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }
    }

    private void validateTotalQuantity(Integer totalQuantity) {
        if (totalQuantity == null || totalQuantity <= 0) {
            throw new IllegalArgumentException("총 수량은 1개 이상이어야 합니다.");
        }
    }




}
