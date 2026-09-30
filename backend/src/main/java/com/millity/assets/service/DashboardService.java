package com.millity.assets.service;

import com.millity.assets.api.DashboardResponse;
import com.millity.assets.domain.*;
import com.millity.assets.security.JwtPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {
    private static final LocalDate SYSTEM_START = LocalDate.of(1900, 1, 1);
    private final EntityManager em;

    public DashboardService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary(LocalDate requestedFromDate, LocalDate requestedToDate,
                                     Long requestedBaseId, String requestedEquipmentType, JwtPrincipal actor) {
        if (actor.role() != Role.ADMIN && actor.role() != Role.BASE_COMMANDER)
            throw new AccessDeniedException("Dashboard access is restricted to admins and base commanders");
        
        LocalDate fromDate = requestedFromDate == null ? SYSTEM_START : requestedFromDate;
        LocalDate toDate = requestedToDate == null ? LocalDate.now() : requestedToDate;
        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be on or before toDate");
        }

        Long baseId = requestedBaseId;
        if (actor.role() == Role.BASE_COMMANDER) {
            if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
            if (requestedBaseId != null && !actor.baseId().equals(requestedBaseId))
                throw new AccessDeniedException("Cannot view another base");
            baseId = actor.baseId();
        }

        String equipmentType = StringUtils.hasText(requestedEquipmentType) ? requestedEquipmentType.trim() : null;

        long current = sumEquipment(baseId, equipmentType);
        long assignmentsToReverse = sumAssignments(baseId, equipmentType, fromDate, null, null);
        long purchasesToReverse = sumPurchases(baseId, equipmentType, fromDate, null);
        long transfersInToReverse = sumTransfersIn(baseId, equipmentType, fromDate, null);
        long transfersOutToReverse = sumTransfersOut(baseId, equipmentType, fromDate, null);
        
        long openingBalance = current + assignmentsToReverse - purchasesToReverse - transfersInToReverse + transfersOutToReverse;

        long purchaseCount = sumPurchases(baseId, equipmentType, fromDate, toDate);
        long transferIn = sumTransfersIn(baseId, equipmentType, fromDate, toDate);
        long transferOut = sumTransfersOut(baseId, equipmentType, fromDate, toDate);
        long assigned = sumAssignments(baseId, equipmentType, fromDate, toDate, null);
        long expended = sumAssignments(baseId, equipmentType, fromDate, toDate, true);
        
        long netMovement = purchaseCount + transferIn - transferOut;
        long closingBalance = openingBalance + netMovement - assigned;

        return new DashboardResponse(fromDate, toDate, baseId, equipmentType, openingBalance, closingBalance,
                netMovement, purchaseCount, transferIn, transferOut, assigned, expended);
    }

    private long sumEquipment(Long baseId, String equipmentType) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Equipment> root = cq.from(Equipment.class);
        List<Predicate> predicates = new ArrayList<>();
        if (baseId != null) predicates.add(cb.equal(root.get("base").get("id"), baseId));
        if (equipmentType != null) predicates.add(cb.equal(cb.lower(root.get("type")), equipmentType.toLowerCase()));
        cq.select(cb.coalesce(cb.sumAsLong(root.get("quantity")), 0L)).where(predicates.toArray(Predicate[]::new));
        Long res = em.createQuery(cq).getSingleResult();
        return res != null ? res : 0L;
    }

    private long sumPurchases(Long baseId, String equipmentType, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Purchase> root = cq.from(Purchase.class);
        List<Predicate> predicates = new ArrayList<>();
        if (baseId != null) predicates.add(cb.equal(root.get("base").get("id"), baseId));
        if (equipmentType != null) predicates.add(cb.equal(cb.lower(root.get("equipmentType")), equipmentType.toLowerCase()));
        if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
        if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
        cq.select(cb.coalesce(cb.sumAsLong(root.get("quantity")), 0L)).where(predicates.toArray(Predicate[]::new));
        Long res = em.createQuery(cq).getSingleResult();
        return res != null ? res : 0L;
    }

    private long sumTransfersIn(Long baseId, String equipmentType, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Transfer> root = cq.from(Transfer.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), TransferStatus.COMPLETED));
        if (baseId != null) predicates.add(cb.equal(root.get("toBase").get("id"), baseId));
        if (equipmentType != null) predicates.add(cb.equal(cb.lower(root.get("equipmentType")), equipmentType.toLowerCase()));
        if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
        if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
        cq.select(cb.coalesce(cb.sumAsLong(root.get("quantity")), 0L)).where(predicates.toArray(Predicate[]::new));
        Long res = em.createQuery(cq).getSingleResult();
        return res != null ? res : 0L;
    }

    private long sumTransfersOut(Long baseId, String equipmentType, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Transfer> root = cq.from(Transfer.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), TransferStatus.COMPLETED));
        if (baseId != null) predicates.add(cb.equal(root.get("fromBase").get("id"), baseId));
        if (equipmentType != null) predicates.add(cb.equal(cb.lower(root.get("equipmentType")), equipmentType.toLowerCase()));
        if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
        if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
        cq.select(cb.coalesce(cb.sumAsLong(root.get("quantity")), 0L)).where(predicates.toArray(Predicate[]::new));
        Long res = em.createQuery(cq).getSingleResult();
        return res != null ? res : 0L;
    }

    private long sumAssignments(Long baseId, String equipmentType, LocalDate fromDate, LocalDate toDate, Boolean expended) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Assignment> root = cq.from(Assignment.class);
        List<Predicate> predicates = new ArrayList<>();
        if (baseId != null) predicates.add(cb.equal(root.get("base").get("id"), baseId));
        if (equipmentType != null) predicates.add(cb.equal(cb.lower(root.get("equipmentType")), equipmentType.toLowerCase()));
        if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
        if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
        if (expended != null) predicates.add(cb.equal(root.get("expended"), expended));
        cq.select(cb.coalesce(cb.sumAsLong(root.get("quantity")), 0L)).where(predicates.toArray(Predicate[]::new));
        Long res = em.createQuery(cq).getSingleResult();
        return res != null ? res : 0L;
    }
}
