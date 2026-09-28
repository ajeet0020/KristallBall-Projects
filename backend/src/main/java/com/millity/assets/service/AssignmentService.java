package com.millity.assets.service;

import com.millity.assets.api.AssignmentRequest;
import com.millity.assets.api.AssignmentResponse;
import com.millity.assets.api.EquipmentOption;
import com.millity.assets.domain.*;
import com.millity.assets.repository.*;
import com.millity.assets.security.JwtPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssignmentService {
    private final AssignmentRepository assignments;
    private final BaseRepository bases;
    private final EquipmentRepository equipment;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;

    public AssignmentService(AssignmentRepository assignments, BaseRepository bases, EquipmentRepository equipment,
                             UserRepository users, AuditLogRepository auditLogs) {
        this.assignments=assignments; this.bases=bases; this.equipment=equipment; this.users=users; this.auditLogs=auditLogs;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long requestedBaseId, LocalDate fromDate, LocalDate toDate,
                                         String equipmentType, String personnel, Boolean expended, JwtPrincipal actor) {
        Long scopedBaseId = scopeBase(requestedBaseId, actor);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be on or before toDate");
        Specification<Assignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (scopedBaseId != null) predicates.add(cb.equal(root.get("base").get("id"), scopedBaseId));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
            if (StringUtils.hasText(equipmentType)) predicates.add(cb.equal(cb.lower(root.get("equipmentType")), equipmentType.trim().toLowerCase()));
            if (StringUtils.hasText(personnel)) predicates.add(cb.like(cb.lower(root.get("assignedToPersonnel")), "%" + personnel.trim().toLowerCase() + "%"));
            if (expended != null) predicates.add(cb.equal(root.get("expended"), expended));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return assignments.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(Long id, JwtPrincipal actor) {
        Assignment assignment = findAssignment(id);
        requireBaseAccess(assignment.getBase().getId(), actor);
        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse create(AssignmentRequest request, JwtPrincipal actor) {
        Base base = resolveBase(request.baseId(), actor);
        String type = request.equipmentType().trim();
        Equipment stock = equipment.findFirstByBase_IdAndTypeIgnoreCase(base.getId(), type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment type not found at this base"));
        if (stock.getStatus() == EquipmentStatus.UNAVAILABLE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This equipment is currently unavailable for assignment");
        if (stock.getQuantity() < request.quantity())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested quantity exceeds the available balance of " + stock.getQuantity());

        User actorUser = findUser(actor.id());
        stock.reduceQuantity(request.quantity());
        Assignment assignment = assignments.save(new Assignment(base, stock.getType(), request.quantity(),
                request.assignedToPersonnel().trim(), request.date(), false));
        writeAudit(actorUser, "ASSIGNMENT_CREATED", assignment);
        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse markExpended(Long id, JwtPrincipal actor) {
        Assignment assignment = findAssignment(id);
        requireBaseAccess(assignment.getBase().getId(), actor);
        if (Boolean.TRUE.equals(assignment.getExpended()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assignment is already marked as expended");
        assignment.markExpended();
        User actorUser = findUser(actor.id());
        String payload = "{\"assignmentId\":" + assignment.getId() + ",\"baseId\":" + assignment.getBase().getId()
                + ",\"equipmentType\":\"" + escape(assignment.getEquipmentType()) + "\",\"quantity\":"
                + assignment.getQuantity() + ",\"expended\":true}";
        auditLogs.save(new AuditLog(actorUser, "ASSIGNMENT_MARKED_EXPENDED", "/api/assignments/" + id + "/expend", payload, Instant.now()));
        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<Base> listBases(JwtPrincipal actor) {
        if (actor.role() == Role.ADMIN) return bases.findAll(Sort.by("name"));
        if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
        return List.of(findBase(actor.baseId()));
    }

    @Transactional(readOnly = true)
    public List<EquipmentOption> equipmentAtBase(Long requestedBaseId, JwtPrincipal actor) {
        Long baseId = scopeBase(requestedBaseId, actor);
        if (baseId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseId is required");
        return equipment.findByBase_IdOrderByTypeAscNameAsc(baseId).stream()
                .map(item -> new EquipmentOption(item.getId(), item.getBase().getId(), item.getType(), item.getName(), item.getQuantity(), item.getStatus()))
                .toList();
    }

    private Long scopeBase(Long requestedBaseId, JwtPrincipal actor) {
        if (actor.role() == Role.ADMIN) return requestedBaseId;
        if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
        if (requestedBaseId != null && !actor.baseId().equals(requestedBaseId))
            throw new AccessDeniedException("Cannot access another base");
        return actor.baseId();
    }

    private Base resolveBase(Long requestedBaseId, JwtPrincipal actor) {
        Long baseId = actor.role() == Role.ADMIN ? requestedBaseId : actor.baseId();
        if (baseId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseId is required for ADMIN assignments");
        if (actor.role() != Role.ADMIN && requestedBaseId != null && !actor.baseId().equals(requestedBaseId))
            throw new AccessDeniedException("Cannot assign assets at another base");
        return findBase(baseId);
    }

    private void requireBaseAccess(Long baseId, JwtPrincipal actor) {
        if (actor.role() != Role.ADMIN && !baseId.equals(actor.baseId()))
            throw new AccessDeniedException("Cannot access another base");
    }

    private Assignment findAssignment(Long id) {
        return assignments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private Base findBase(Long id) {
        return bases.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Base not found"));
    }

    private User findUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private void writeAudit(User actor, String action, Assignment assignment) {
        String payload = "{\"assignmentId\":" + assignment.getId() + ",\"baseId\":" + assignment.getBase().getId()
                + ",\"equipmentType\":\"" + escape(assignment.getEquipmentType()) + "\",\"quantity\":"
                + assignment.getQuantity() + ",\"assignedToPersonnel\":\"" + escape(assignment.getAssignedToPersonnel())
                + "\",\"date\":\"" + assignment.getDate() + "\",\"expended\":false}";
        auditLogs.save(new AuditLog(actor, action, "/api/assignments/" + assignment.getId(), payload, Instant.now()));
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getBase().getId(), assignment.getBase().getName(),
                assignment.getEquipmentType(), assignment.getQuantity(), assignment.getAssignedToPersonnel(),
                assignment.getDate(), assignment.getExpended(), assignment.getCreatedAt());
    }
}
