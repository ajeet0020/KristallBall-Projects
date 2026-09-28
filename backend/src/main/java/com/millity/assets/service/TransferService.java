package com.millity.assets.service;

import com.millity.assets.api.TransferRequest;
import com.millity.assets.api.TransferResponse;
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
public class TransferService {
    private final TransferRepository transfers;
    private final BaseRepository bases;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;

    public TransferService(TransferRepository transfers, BaseRepository bases, UserRepository users, AuditLogRepository auditLogs) {
        this.transfers=transfers; this.bases=bases; this.users=users; this.auditLogs=auditLogs;
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> list(Long baseFilterId, LocalDate fromDate, LocalDate toDate,
                                       String equipmentType, TransferStatus status, JwtPrincipal actor) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be on or before toDate");
        Specification<Transfer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actor.role() != Role.ADMIN) {
                if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
                predicates.add(cb.or(cb.equal(root.get("fromBase").get("id"), actor.baseId()),
                        cb.equal(root.get("toBase").get("id"), actor.baseId())));
            }
            if (baseFilterId != null) predicates.add(cb.or(cb.equal(root.get("fromBase").get("id"), baseFilterId),
                    cb.equal(root.get("toBase").get("id"), baseFilterId)));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
            if (StringUtils.hasText(equipmentType)) predicates.add(cb.equal(cb.lower(root.get("equipmentType")), equipmentType.trim().toLowerCase()));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return transfers.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse get(Long id, JwtPrincipal actor) {
        Transfer transfer = findTransfer(id);
        requireTransferAccess(transfer, actor);
        return toResponse(transfer);
    }

    @Transactional
    public TransferResponse create(TransferRequest request, JwtPrincipal actor) {
        Long fromId = request.fromBaseId();
        if (actor.role() != Role.ADMIN) {
            if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
            if (!actor.baseId().equals(fromId)) throw new AccessDeniedException("Transfers must originate from your assigned base");
            fromId = actor.baseId();
        }
        if (request.fromBaseId().equals(request.toBaseId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromBaseId and toBaseId must be different");
        Base fromBase = findBase(fromId);
        Base toBase = findBase(request.toBaseId());
        User creator = findUser(actor.id());
        Transfer transfer = transfers.save(new Transfer(fromBase, toBase, request.equipmentType().trim(), request.quantity(),
                request.date(), TransferStatus.PENDING, creator));
        writeAudit(creator, "TRANSFER_CREATED", transfer, null, transfer.getStatus());
        return toResponse(transfer);
    }

    @Transactional
    public TransferResponse updateStatus(Long id, TransferStatus newStatus, JwtPrincipal actor) {
        Transfer transfer = findTransfer(id);
        requireTransferAccess(transfer, actor);
        TransferStatus oldStatus = transfer.getStatus();
        if (!allowedNextStatuses(oldStatus).contains(newStatus))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid transfer status transition from " + oldStatus + " to " + newStatus);
        transfer.setStatus(newStatus);
        User updater = findUser(actor.id());
        writeAudit(updater, "TRANSFER_STATUS_UPDATED", transfer, oldStatus, newStatus);
        return toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<Base> listDestinationBases(JwtPrincipal actor) {
        // Transfers need a destination directory; this exposes base metadata only, never inventory.
        if (actor.role() != Role.ADMIN && actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
        return bases.findAll(Sort.by("name"));
    }

    private List<TransferStatus> allowedNextStatuses(TransferStatus current) {
        return switch (current) {
            case PENDING -> List.of(TransferStatus.IN_TRANSIT, TransferStatus.REJECTED);
            case IN_TRANSIT -> List.of(TransferStatus.COMPLETED, TransferStatus.REJECTED);
            case COMPLETED, REJECTED -> List.of();
        };
    }

    private void requireTransferAccess(Transfer transfer, JwtPrincipal actor) {
        if (actor.role() == Role.ADMIN) return;
        if (actor.baseId() == null || (!actor.baseId().equals(transfer.getFromBase().getId())
                && !actor.baseId().equals(transfer.getToBase().getId())))
            throw new AccessDeniedException("Transfer does not involve your assigned base");
    }

    private Transfer findTransfer(Long id) {
        return transfers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found"));
    }

    private Base findBase(Long id) {
        return bases.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Base not found"));
    }

    private User findUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private void writeAudit(User actor, String action, Transfer transfer, TransferStatus oldStatus, TransferStatus newStatus) {
        StringBuilder payload = new StringBuilder("{\"transferId\":").append(transfer.getId())
                .append(",\"fromBaseId\":").append(transfer.getFromBase().getId())
                .append(",\"toBaseId\":").append(transfer.getToBase().getId())
                .append(",\"equipmentType\":\"").append(transfer.getEquipmentType().replace("\\", "\\\\").replace("\"", "\\\""))
                .append("\",\"quantity\":").append(transfer.getQuantity())
                .append(",\"date\":\"").append(transfer.getDate()).append("\",\"status\":\"").append(newStatus).append("\"");
        if (oldStatus != null) payload.append(",\"previousStatus\":\"").append(oldStatus).append("\"");
        payload.append('}');
        auditLogs.save(new AuditLog(actor, action, "/api/transfers/" + transfer.getId(), payload.toString(), Instant.now()));
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getFromBase().getId(), transfer.getFromBase().getName(),
                transfer.getToBase().getId(), transfer.getToBase().getName(), transfer.getEquipmentType(),
                transfer.getQuantity(), transfer.getDate(), transfer.getStatus(), transfer.getCreatedAt(),
                transfer.getCreatedBy().getId(), transfer.getCreatedBy().getUsername());
    }
}
