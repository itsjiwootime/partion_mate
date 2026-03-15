package com.project.partition_mate.controller;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.service.PartyService;
import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/party")
public class PartyController {

    private final PartyService partyService;

    @PostMapping
    public ResponseEntity<PartyResponse> createParty(@RequestBody @Valid CreatePartyRequest createPartyRequest) {
        Party party = partyService.createParty(createPartyRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(PartyResponse.from(party));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<JoinPartyResponse> joinParty(@PathVariable Long id,
                                                       @RequestBody @Valid JoinPartyRequest joinPartyRequest) {
        JoinPartyResponse joinPartyResponse = partyService.joinParty(id, joinPartyRequest);

        HttpStatus status = joinPartyResponse.isWaiting() ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(joinPartyResponse);
    }

    @DeleteMapping("/{id}/join")
    public ResponseEntity<Void> cancelJoin(@PathVariable Long id) {
        partyService.cancelJoin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<PartyResponse>> getAllParty() {
        List<Party> partyList = partyService.getAllParties();

        return ResponseEntity.ok(partyList.stream().map(PartyResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyDetailResponse> getPartyDetailById(@PathVariable Long id) {
        PartyDetailResponse response = partyService.detailsParty(id);

        return ResponseEntity.ok(response);

    }


}
