package com.millity.assets.service;

import com.millity.assets.api.PurchaseRequest;
import com.millity.assets.api.PurchaseResponse;
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
public class PurchaseService {
    private final PurchaseRepository purchases;
    private final BaseRepository bases;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;

    public PurchaseService(PurchaseRepository purchases, BaseRepository bases, UserRepository users, AuditLogRepository auditLogs) {
        this.purchases = purchases; this.bases = bases; this.users = users; this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> list(Long requestedBaseId, LocalDate fromDate, LocalDate toDate, String equipmentType, JwtPrincipal actor) {
        Long scopedBaseId = scopeBase(requestedBaseId, actor);
        Specification<Purchase> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (scopedBaseId != null) predicates.add(cb.equal(root.get("base").get("id"), scopedBaseId));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
            if (StringUtils.hasText(equipmentType)) predicates.add(cb.equal(cb.lower(root.get("equipmentType")), equipmentType.trim().toLowerCase()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return purchases.findAll(spec, Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(Long id, JwtPrincipal actor) {
        Purchase purchase = findPurchase(id);
        requireBaseAccess(purchase.getBase().getId(), actor);
        return toResponse(purchase);
    }

    @Transactional
    public PurchaseResponse create(PurchaseRequest request, JwtPrincipal actor) {
        Base base = resolveBase(request.baseId(), actor);
        User creator = findUser(actor.id());
        Purchase purchase = purchases.save(new Purchase(base, cleanEquipmentType(request.equipmentType()), request.quantity(), request.date(), creator));
        writeAudit(creator, "PURCHASE_CREATED", purchase);
        return toResponse(purchase);
    }

    @Transactional
    public PurchaseResponse update(Long id, PurchaseRequest request, JwtPrincipal actor) {
        Purchase purchase = findPurchase(id);
        requireBaseAccess(purchase.getBase().getId(), actor);
        Base base = resolveBase(request.baseId(), actor);
        User creator = findUser(actor.id());
        purchase.setDetails(base, cleanEquipmentType(request.equipmentType()), request.quantity(), request.date());
        writeAudit(creator, "PURCHASE_UPDATED", purchase);
        return toResponse(purchase);
    }

    @Transactional
    public void delete(Long id, JwtPrincipal actor) {
        Purchase purchase = findPurchase(id);
        requireBaseAccess(purchase.getBase().getId(), actor);
        User user = findUser(actor.id());
        String payload = "{\"purchaseId\":" + purchase.getId() + ",\"baseId\":" + purchase.getBase().getId() + "}";
        auditLogs.save(new AuditLog(user, "PURCHASE_DELETED", "/api/purchases/" + id, payload, Instant.now()));
        purchases.delete(purchase);
    }

    @Transactional(readOnly = true)
    public List<Base> listBases(JwtPrincipal actor) {
        if (actor.role() == Role.ADMIN) return bases.findAll(Sort.by("name"));
        if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
        return List.of(bases.findById(actor.baseId()).orElseThrow(() -> new AccessDeniedException("Assigned base does not exist")));
    }

    private Long scopeBase(Long requestedBaseId, JwtPrincipal actor) {
        if (actor.role() == Role.ADMIN) return requestedBaseId;
        if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
        if (requestedBaseId != null && !actor.baseId().equals(requestedBaseId)) throw new AccessDeniedException("Cannot access another base");
        return actor.baseId();
    }

    private Base resolveBase(Long requestedBaseId, JwtPrincipal actor) {
        Long baseId = actor.role() == Role.ADMIN ? requestedBaseId : actor.baseId();
        if (baseId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseId is required for ADMIN purchases");
        if (actor.role() != Role.ADMIN && requestedBaseId != null && !actor.baseId().equals(requestedBaseId))
            throw new AccessDeniedException("Cannot create a purchase for another base");
        return bases.findById(baseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Base not found"));
    }

    private void requireBaseAccess(Long baseId, JwtPrincipal actor) {
        if (actor.role() != Role.ADMIN && !baseId.equals(actor.baseId())) throw new AccessDeniedException("Cannot access another base");
    }

    private Purchase findPurchase(Long id) {
        return purchases.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found"));
    }

    private User findUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private String cleanEquipmentType(String equipmentType) { return equipmentType.trim(); }

    private void writeAudit(User actor, String action, Purchase purchase) {
        String payload = "{\"purchaseId\":" + purchase.getId() + ",\"baseId\":" + purchase.getBase().getId()
                + ",\"equipmentType\":\"" + purchase.getEquipmentType().replace("\\", "\\\\").replace("\"", "\\\"")
                + "\",\"quantity\":" + purchase.getQuantity() + ",\"date\":\"" + purchase.getDate() + "\"}";
        auditLogs.save(new AuditLog(actor, action, "/api/purchases/" + purchase.getId(), payload, Instant.now()));
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        return new PurchaseResponse(purchase.getId(), purchase.getBase().getId(), purchase.getBase().getName(),
                purchase.getEquipmentType(), purchase.getQuantity(), purchase.getDate(),
                purchase.getCreatedBy().getId(), purchase.getCreatedBy().getUsername());
    }
}
