package com.project.partition_mate.service;

import com.project.partition_mate.domain.FavoriteParty;
import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.repository.FavoritePartyRepository;
import com.project.partition_mate.repository.PartyRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FavoritePartyService {

    private final FavoritePartyRepository favoritePartyRepository;
    private final PartyRepository partyRepository;
    private final Clock clock;

    @Transactional
    public void save(User user, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        if (favoritePartyRepository.existsByUserAndParty(user, party)) {
            return;
        }

        favoritePartyRepository.save(FavoriteParty.create(user, party, LocalDateTime.now(clock)));
    }

    @Transactional
    public void remove(User user, Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다"));

        favoritePartyRepository.findByUserAndParty(user, party)
                .ifPresent(favoritePartyRepository::delete);
    }

    public boolean isFavorite(User user, Party party) {
        return favoritePartyRepository.existsByUserAndParty(user, party);
    }

    public List<PartyResponse> getFavoriteParties(User user) {
        return favoritePartyRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(favoriteParty -> PartyResponse.from(favoriteParty.getParty()).withFavorite(true))
                .toList();
    }

    public List<PartyResponse> applyFavoriteFlags(User user, List<PartyResponse> responses) {
        if (user == null || responses.isEmpty()) {
            return responses;
        }

        Set<Long> favoriteIds = resolveFavoriteIds(user, responses.stream().map(PartyResponse::getId).toList());
        return responses.stream()
                .map(response -> response.withFavorite(favoriteIds.contains(response.getId())))
                .toList();
    }

    public Set<Long> resolveFavoriteIds(User user, Collection<Long> partyIds) {
        if (user == null || partyIds.isEmpty()) {
            return Set.of();
        }
        return favoritePartyRepository.findPartyIdsByUserAndPartyIdIn(user, partyIds);
    }
}
