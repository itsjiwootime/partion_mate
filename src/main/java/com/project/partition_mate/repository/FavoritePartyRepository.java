package com.project.partition_mate.repository;

import com.project.partition_mate.domain.FavoriteParty;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoritePartyRepository extends JpaRepository<FavoriteParty, Long> {

    Optional<FavoriteParty> findByUserAndParty(User user, Party party);

    boolean existsByUserAndParty(User user, Party party);

    @EntityGraph(attributePaths = {"party", "party.store", "party.members", "party.members.user"})
    List<FavoriteParty> findAllByUserOrderByCreatedAtDesc(User user);

    @Query("""
            select fp.party.id
            from FavoriteParty fp
            where fp.user = :user
              and fp.party.id in :partyIds
            """)
    Set<Long> findPartyIdsByUserAndPartyIdIn(@Param("user") User user, @Param("partyIds") Collection<Long> partyIds);
}
