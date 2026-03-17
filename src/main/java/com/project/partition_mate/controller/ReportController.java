package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.CreateReportRequest;
import com.project.partition_mate.dto.ReportResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(@RequestBody @Valid CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(getCurrentUser(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReportResponse>> getMyReports() {
        return ResponseEntity.ok(reportService.getMyReports(getCurrentUser()));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return principal.getUser();
    }
}
