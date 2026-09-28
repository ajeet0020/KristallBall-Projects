package com.millity.assets.api;

import com.millity.assets.domain.Base;
import com.millity.assets.security.JwtPrincipal;
import com.millity.assets.service.PurchaseService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','BASE_COMMANDER','LOGISTICS_OFFICER')")
public class PurchaseController {
    private final PurchaseService service;
    public PurchaseController(PurchaseService service) { this.service = service; }

    @GetMapping("/purchases")
    public List<PurchaseResponse> list(
            @RequestParam(required = false) Long baseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String equipmentType,
            @AuthenticationPrincipal JwtPrincipal actor) {
        validateDateRange(fromDate, toDate);
        return service.list(baseId, fromDate, toDate, equipmentType, actor);
    }

    @GetMapping("/purchases/{id}")
    public PurchaseResponse get(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal actor) {
        return service.get(id, actor);
    }

    @PostMapping("/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse create(@Valid @RequestBody PurchaseRequest request, @AuthenticationPrincipal JwtPrincipal actor) {
        return service.create(request, actor);
    }

    @PutMapping("/purchases/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BASE_COMMANDER')")
    public PurchaseResponse update(@PathVariable Long id, @Valid @RequestBody PurchaseRequest request,
                                   @AuthenticationPrincipal JwtPrincipal actor) {
        return service.update(id, request, actor);
    }

    @DeleteMapping("/purchases/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BASE_COMMANDER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal actor) {
        service.delete(id, actor);
    }

    @GetMapping("/bases")
    public List<BaseOption> bases(@AuthenticationPrincipal JwtPrincipal actor) {
        return service.listBases(actor).stream().map(base -> new BaseOption(base.getId(), base.getName(), base.getLocation())).toList();
    }

    public record BaseOption(Long id, String name, String location) {}

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be on or before toDate");
    }
}
