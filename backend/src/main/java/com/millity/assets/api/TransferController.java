package com.millity.assets.api;

import com.millity.assets.domain.Base;
import com.millity.assets.domain.TransferStatus;
import com.millity.assets.security.JwtPrincipal;
import com.millity.assets.service.TransferService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_BASE_COMMANDER','ROLE_LOGISTICS_OFFICER')")
public class TransferController {
    private final TransferService service;
    public TransferController(TransferService service) { this.service=service; }

    @GetMapping
    public List<TransferResponse> list(
            @RequestParam(required = false) Long baseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) TransferStatus status,
            @AuthenticationPrincipal JwtPrincipal actor) {
        return service.list(baseId, fromDate, toDate, equipmentType, status, actor);
    }

    @GetMapping("/bases")
    public List<BaseOption> bases(@AuthenticationPrincipal JwtPrincipal actor) {
        return service.listDestinationBases(actor).stream().map(base -> new BaseOption(base.getId(), base.getName(), base.getLocation())).toList();
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal actor) {
        return service.get(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody TransferRequest request, @AuthenticationPrincipal JwtPrincipal actor) {
        return service.create(request, actor);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_BASE_COMMANDER')")
    public TransferResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TransferStatusRequest request,
                                        @AuthenticationPrincipal JwtPrincipal actor) {
        return service.updateStatus(id, request.status(), actor);
    }

    public record BaseOption(Long id, String name, String location) {}
}
