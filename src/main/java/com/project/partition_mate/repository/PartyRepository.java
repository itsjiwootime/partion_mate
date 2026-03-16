package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.dto.PartyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    List<Party> findByPartyStatus(PartyStatus status);

    List<Party> findAllByStoreIdAndPartyStatus(Long storeId, PartyStatus status);

    long countByStoreIdAndPartyStatus(Long storeId, PartyStatus status);

    @Query("""
            select new com.project.partition_mate.dto.PartyResponse(
                p.id,
                p.title,
                p.productName,
                coalesce(p.actualTotalPrice, p.totalPrice),
                p.totalPrice,
                p.actualTotalPrice,
                p.totalQuantity,
                p.partyStatus,
                s.name,
                coalesce(sum(pm.requestedQuantity), 0L),
                p.openChatUrl,
                p.unitLabel,
                p.minimumShareUnit,
                p.storageType,
                p.packagingType,
                p.hostProvidesPackaging,
                p.onSiteSplit,
                p.guideNote,
                p.receiptNote,
                p.deadline,
                p.closedAt,
                p.closeReason
            )
            from Party p
            join p.store s
            left join p.members pm
            where s.id = :storeId
            group by p.id, p.title, p.productName, p.totalPrice, p.actualTotalPrice, p.totalQuantity, p.partyStatus, s.name, p.openChatUrl, p.unitLabel, p.minimumShareUnit, p.storageType, p.packagingType, p.hostProvidesPackaging, p.onSiteSplit, p.guideNote, p.receiptNote, p.deadline, p.closedAt, p.closeReason
            order by p.id desc
            """)
    List<PartyResponse> findResponsesByStoreId(@Param("storeId") Long storeId);

    List<Party> findAllByDeadlineLessThanEqualAndPartyStatusIn(LocalDateTime deadline, Collection<PartyStatus> partyStatuses);

    @Query("""
            select distinct p
            from Party p
            join fetch p.store
            left join fetch p.members
            where p.id = :id
            """)
    Optional<Party> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Party p where p.id = :id")
    Optional<Party> findByIdForUpdate(@Param("id") Long id);

}
