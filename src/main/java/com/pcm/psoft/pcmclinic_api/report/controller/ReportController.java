package com.pcm.psoft.pcmclinic_api.report.controller;

import com.pcm.psoft.pcmclinic_api.auth.api.AuthHelper;
import com.pcm.psoft.pcmclinic_api.report.dto.MonthlyReportDTO;
import com.pcm.psoft.pcmclinic_api.report.dto.AgeGroupStatsDTO;
import com.pcm.psoft.pcmclinic_api.report.service.ReportService;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.User;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Report generation endpoints")
public class ReportController {
    private final ReportService reportService;
    private final AuthHelper authHelper;

    public ReportController(ReportService reportService, AuthHelper authHelper) {
        this.reportService = reportService;
        this.authHelper = authHelper;
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Get monthly appointments report",
        description = "Generate a report of appointments for a specific month",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid month format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<?> getMonthlyReport(@RequestParam String month) {
        User currentUser = authHelper.getCurrentUser();
        
        if (!authHelper.isAdmin()) {
            return ResponseEntity.status(403).body("Only administrators can access reports");
        }

        try {
            MonthlyReportDTO report = reportService.generateMonthlyReport(month);
            return ResponseEntity.ok(Collections.singletonList(report));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid month format. Use YYYY-MM");
        }
    }

    @GetMapping("/age-groups")
    public ResponseEntity<?> getAppointmentStatsByAgeGroup() {
        if (!authHelper.isAdmin()) {
            return ResponseEntity.status(403).body("Only administrators can access this resource");
        }
        List<AgeGroupStatsDTO> stats = reportService.getAppointmentStatsByAgeGroup();
        return ResponseEntity.ok(stats);
    }
} 