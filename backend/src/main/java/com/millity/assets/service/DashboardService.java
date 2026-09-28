package com.millity.assets.service;

import com.millity.assets.api.DashboardResponse;
import com.millity.assets.domain.Role;
import com.millity.assets.repository.AssignmentRepository;
import com.millity.assets.repository.EquipmentRepository;
import com.millity.assets.repository.PurchaseRepository;
import com.millity.assets.repository.TransferRepository;
import com.millity.assets.security.JwtPrincipal;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {
    private static final LocalDate SYSTEM_START = LocalDate.of(1900, 1, 1);
    private final EquipmentRepository equipment;
    private final PurchaseRepository purchases;
    private final TransferRepository transfers;
    private final AssignmentRepository assignments;

    public DashboardService(EquipmentRepository equipment, PurchaseRepository purchases,
                            TransferRepository transfers, AssignmentRepository assignments) {
        this.equipment=equipment; this.purchases=purchases; this.transfers=transfers; this.assignments=assignments;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary(LocalDate requestedFromDate, LocalDate requestedToDate,
                                     Long requestedBaseId, String requestedEquipmentType, JwtPrincipal actor) {
        if (actor.role() != Role.ADMIN && actor.role() != Role.BASE_COMMANDER)
            throw new AccessDeniedException("Dashboard access is restricted to admins and base commanders");
        LocalDate fromDate = requestedFromDate == null ? SYSTEM_START : requestedFromDate;
        LocalDate toDate = requestedToDate == null ? LocalDate.now() : requestedToDate;
        if (fromDate.isAfter(toDate)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be on or before toDate");
        Long baseId = requestedBaseId;
        if (actor.role() == Role.BASE_COMMANDER) {
            if (actor.baseId() == null) throw new AccessDeniedException("User has no assigned base");
            if (requestedBaseId != null && !actor.baseId().equals(requestedBaseId))
                throw new AccessDeniedException("Cannot view another base");
            baseId = actor.baseId();
        }
        String equipmentType = StringUtils.hasText(requestedEquipmentType) ? requestedEquipmentType.trim() : null;

        long current = value(equipment.sumCurrentQuantity(baseId, equipmentType));
        long assignmentsToReverse = value(assignments.sumOnOrAfter(baseId, equipmentType, fromDate));
        long purchasesToReverse = value(purchases.sumOnOrAfter(baseId, equipmentType, fromDate));
        long transfersInToReverse = value(transfers.sumInboundOnOrAfter(baseId, equipmentType, fromDate));
        long transfersOutToReverse = value(transfers.sumOutboundOnOrAfter(baseId, equipmentType, fromDate));
        long openingBalance = current + assignmentsToReverse - purchasesToReverse - transfersInToReverse + transfersOutToReverse;

        long purchaseCount = value(purchases.sumBetween(baseId, equipmentType, fromDate, toDate));
        long transferIn = value(transfers.sumInboundBetween(baseId, equipmentType, fromDate, toDate));
        long transferOut = value(transfers.sumOutboundBetween(baseId, equipmentType, fromDate, toDate));
        long assigned = value(assignments.sumBetween(baseId, equipmentType, fromDate, toDate));
        long expended = value(assignments.sumExpendedBetween(baseId, equipmentType, fromDate, toDate));
        long netMovement = purchaseCount + transferIn - transferOut;
        long closingBalance = openingBalance + netMovement - assigned;

        return new DashboardResponse(fromDate, toDate, baseId, equipmentType, openingBalance, closingBalance,
                netMovement, purchaseCount, transferIn, transferOut, assigned, expended);
    }

    private long value(Long value) { return value == null ? 0L : value; }
}
