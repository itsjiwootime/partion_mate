package com.project.partition_mate.repository;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.PartyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    List<Party> findAllByStoreId(Long storeId);

    List<Party> findByPartyStatus(PartyStatus status);

    List<Party> findAllByStoreIdAndPartyStatus(Long storeId, PartyStatus status);

    long countByStoreIdAndPartyStatus(Long storeId, PartyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Party p where p.id = :id")
    Optional<Party> findByIdForUpdate(@Param("id") Long id);

}
