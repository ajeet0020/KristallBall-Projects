package com.millity.assets.api;

import com.millity.assets.security.JwtPrincipal;
import com.millity.assets.service.DashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_BASE_COMMANDER')")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard) { this.dashboard=dashboard; }

    @GetMapping
    public DashboardResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long baseId,
            @RequestParam(required = false) String equipmentType,
            @AuthenticationPrincipal JwtPrincipal actor) {
        return dashboard.summary(fromDate, toDate, baseId, equipmentType, actor);
    }
}
