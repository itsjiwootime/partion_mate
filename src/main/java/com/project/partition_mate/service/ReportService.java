package com.project.partition_mate.service;

import com.project.partition_mate.domain.Party;
import com.project.partition_mate.domain.Report;
import com.project.partition_mate.domain.ReportStatus;
import com.project.partition_mate.domain.ReportTargetType;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.CreateReportRequest;
import com.project.partition_mate.dto.ReportResponse;
import com.project.partition_mate.exception.BusinessException;
import com.project.partition_mate.repository.PartyMemberRepository;
import com.project.partition_mate.repository.PartyRepository;
import com.project.partition_mate.repository.ReportRepository;
import com.project.partition_mate.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final Clock clock;

    @Transactional
    public ReportResponse createReport(User reporter, CreateReportRequest request) {
        ValidationContext context = validateAndResolve(reporter, request);

        boolean duplicated = reportRepository.existsDuplicate(
                reporter,
                request.getTargetType(),
                context.party() != null ? context.party().getId() : null,
                context.targetUser() != null ? context.targetUser().getId() : null,
                request.getReasonType(),
                ReportStatus.RECEIVED
        );
        if (duplicated) {
            throw BusinessException.duplicateReport();
        }

        Report report = reportRepository.save(Report.create(
                reporter,
                context.party(),
                context.targetUser(),
                request.getTargetType(),
                request.getReasonType(),
                request.getMemo(),
                LocalDateTime.now(clock)
        ));

        return ReportResponse.from(report);
    }

    public List<ReportResponse> getMyReports(User reporter) {
        return reportRepository.findAllByReporterOrderByCreatedAtDesc(reporter).stream()
                .map(ReportResponse::from)
                .toList();
    }

    private ValidationContext validateAndResolve(User reporter, CreateReportRequest request) {
        Party party = null;
        if (request.getPartyId() != null) {
            party = partyRepository.findById(request.getPartyId())
                    .orElseThrow(() -> new EntityNotFoundException("파티가 존재하지 않습니다."));
        }

        switch (request.getTargetType()) {
            case PARTY -> {
                if (party == null) {
                    throw BusinessException.reportPartyRequired();
                }
                if (request.getTargetUserId() != null) {
                    throw BusinessException.reportTargetUserNotAllowedForParty();
                }
                validateReporterRelatedToParty(reporter, party);
                return new ValidationContext(party, null);
            }
            case USER -> {
                if (party == null) {
                    throw BusinessException.reportPartyRequired();
                }
                validateReporterRelatedToParty(reporter, party);
                User targetUser = validateAndResolveTargetUser(reporter, party, request.getTargetUserId());
                return new ValidationContext(party, targetUser);
            }
            case CHAT -> {
                if (party == null) {
                    throw BusinessException.reportPartyRequired();
                }
                if (!partyMemberRepository.existsByPartyAndUser(party, reporter)) {
                    throw BusinessException.chatReportRequiresJoinedParticipant();
                }
                User targetUser = validateAndResolveTargetUser(reporter, party, request.getTargetUserId());
                if (!partyMemberRepository.existsByPartyAndUser(party, targetUser)) {
                    throw BusinessException.chatReportRequiresJoinedParticipant();
                }
                return new ValidationContext(party, targetUser);
            }
        }

        throw new IllegalStateException("지원하지 않는 신고 대상 타입입니다.");
    }

    private void validateReporterRelatedToParty(User reporter, Party party) {
        if (!partyMemberRepository.existsByPartyAndUser(party, reporter)) {
            throw BusinessException.onlyRelatedUserCanReportParty();
        }
    }

    private User validateAndResolveTargetUser(User reporter, Party party, Long targetUserId) {
        if (targetUserId == null) {
            throw BusinessException.reportTargetUserRequired();
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("신고 대상 사용자가 존재하지 않습니다."));
        if (reporter.getId().equals(targetUserId)) {
            throw BusinessException.selfReportNotAllowed();
        }
        if (!partyMemberRepository.existsByPartyAndUser(party, targetUser)) {
            throw BusinessException.reportTargetUserNotInParty();
        }
        return targetUser;
    }

    private record ValidationContext(Party party, User targetUser) {
    }
}
