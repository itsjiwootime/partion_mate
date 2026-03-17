package com.project.partition_mate.controller;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.dto.ConfirmPickupScheduleRequest;
import com.project.partition_mate.dto.ConfirmSettlementRequest;
import com.project.partition_mate.dto.CreateReviewRequest;
import com.project.partition_mate.dto.CreatePartyRequest;
import com.project.partition_mate.dto.JoinPartyResponse;
import com.project.partition_mate.dto.PartyDetailResponse;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.dto.JoinPartyRequest;
import com.project.partition_mate.dto.UpdatePartyRequest;
import com.project.partition_mate.dto.UpdatePaymentStatusRequest;
import com.project.partition_mate.dto.UpdateTradeStatusRequest;
import com.project.partition_mate.service.PartyService;
import jakarta.validation.Valid;
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

    @PostMapping("/{id}/close")
    public ResponseEntity<PartyDetailResponse> closeParty(@PathVariable Long id) {
        return ResponseEntity.ok(partyService.closeParty(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartyDetailResponse> updateParty(@PathVariable Long id,
                                                           @RequestBody @Valid UpdatePartyRequest updatePartyRequest) {
        return ResponseEntity.ok(partyService.updateParty(id, updatePartyRequest));
    }

    @DeleteMapping("/{id}/join")
    public ResponseEntity<Void> cancelJoin(@PathVariable Long id) {
        partyService.cancelJoin(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/settlement")
    public ResponseEntity<PartyDetailResponse> confirmSettlement(@PathVariable Long id,
                                                                 @RequestBody @Valid ConfirmSettlementRequest request) {
        return ResponseEntity.ok(partyService.confirmSettlement(id, request));
    }

    @PutMapping("/{id}/pickup")
    public ResponseEntity<PartyDetailResponse> confirmPickupSchedule(@PathVariable Long id,
                                                                     @RequestBody @Valid ConfirmPickupScheduleRequest request) {
        return ResponseEntity.ok(partyService.confirmPickupSchedule(id, request));
    }

    @PostMapping("/{id}/pickup/acknowledge")
    public ResponseEntity<PartyDetailResponse> acknowledgePickup(@PathVariable Long id) {
        return ResponseEntity.ok(partyService.acknowledgePickup(id));
    }

    @PutMapping("/{partyId}/members/{memberId}/payment")
    public ResponseEntity<PartyDetailResponse> updatePaymentStatus(@PathVariable Long partyId,
                                                                   @PathVariable Long memberId,
                                                                   @RequestBody @Valid UpdatePaymentStatusRequest request) {
        return ResponseEntity.ok(partyService.updatePaymentStatus(partyId, memberId, request));
    }

    @PutMapping("/{partyId}/members/{memberId}/trade-status")
    public ResponseEntity<PartyDetailResponse> updateTradeStatus(@PathVariable Long partyId,
                                                                 @PathVariable Long memberId,
                                                                 @RequestBody @Valid UpdateTradeStatusRequest request) {
        return ResponseEntity.ok(partyService.updateTradeStatus(partyId, memberId, request));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<PartyDetailResponse> submitReview(@PathVariable Long id,
                                                            @RequestBody @Valid CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyService.submitReview(id, request));
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
