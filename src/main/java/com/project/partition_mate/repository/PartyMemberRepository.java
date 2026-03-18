package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyMember;
import com.project.partition_mate.domain.PartyMemberRole;
import com.project.partition_mate.domain.TradeStatus;
import com.project.partition_mate.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    List<PartyMember> findByParty(Party party);

    List<PartyMember> findAllByPartyId(Long partyId);

    List<PartyMember> findByUser(User user);

    boolean existsByPartyAndUser(Party party, User user);

    boolean existsByPartyIdAndUserId(Long partyId, Long userId);

    Optional<PartyMember> findByPartyAndUser(Party party, User user);

    Optional<PartyMember> findByIdAndParty(Long id, Party party);

    long countByUserAndRoleAndTradeStatus(User user, PartyMemberRole role, TradeStatus tradeStatus);

    @Query("""
            select pm
            from PartyMember pm
            where pm.user = :user
              and pm.role = :role
              and pm.tradeStatus in :tradeStatuses
              and pm.tradeStatusUpdatedAt is not null
            order by pm.tradeStatusUpdatedAt desc, pm.id desc
            """)
    List<PartyMember> findAllForNoShowPolicy(@Param("user") User user,
                                             @Param("role") PartyMemberRole role,
                                             @Param("tradeStatuses") Collection<TradeStatus> tradeStatuses);

    @Query("""
            select count(distinct pm.id)
            from PartyMember pm
            join pm.party p
            join p.members host
            where host.user.id = :hostUserId
              and host.role = com.project.partition_mate.domain.PartyMemberRole.LEADER
              and pm.role = com.project.partition_mate.domain.PartyMemberRole.MEMBER
              and pm.tradeStatus = :tradeStatus
            """)
    long countHostedMemberTradesByStatus(@Param("hostUserId") Long hostUserId,
                                         @Param("tradeStatus") TradeStatus tradeStatus);
}
