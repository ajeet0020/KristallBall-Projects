package com.millity.assets.api;

import com.millity.assets.domain.Base;
import com.millity.assets.security.JwtPrincipal;
import com.millity.assets.service.AssignmentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assignments")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_BASE_COMMANDER')")
public class AssignmentController {
    private final AssignmentService service;
    public AssignmentController(AssignmentService service) { this.service=service; }

    @GetMapping
    public List<AssignmentResponse> list(
            @RequestParam(required = false) Long baseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String personnel,
            @RequestParam(required = false) Boolean expended,
            @AuthenticationPrincipal JwtPrincipal actor) {
        return service.list(baseId, fromDate, toDate, equipmentType, personnel, expended, actor);
    }

    @GetMapping("/bases")
    public List<BaseOption> bases(@AuthenticationPrincipal JwtPrincipal actor) {
        return service.listBases(actor).stream().map(base -> new BaseOption(base.getId(), base.getName())).toList();
    }

    @GetMapping("/equipment")
    public List<EquipmentOption> equipment(@RequestParam(required = false) Long baseId,
                                          @AuthenticationPrincipal JwtPrincipal actor) {
        return service.equipmentAtBase(baseId, actor);
    }

    @GetMapping("/{id}")
    public AssignmentResponse get(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal actor) {
        return service.get(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse create(@Valid @RequestBody AssignmentRequest request, @AuthenticationPrincipal JwtPrincipal actor) {
        return service.create(request, actor);
    }

    @PostMapping("/{id}/expend")
    public AssignmentResponse markExpended(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal actor) {
        return service.markExpended(id, actor);
    }

    public record BaseOption(Long id, String name) {}
}
