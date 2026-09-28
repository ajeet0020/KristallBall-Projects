package com.millity.assets.config;

import com.millity.assets.domain.Assignment;
import com.millity.assets.domain.AuditLog;
import com.millity.assets.domain.Equipment;
import com.millity.assets.repository.AssignmentRepository;
import com.millity.assets.repository.AuditLogRepository;
import com.millity.assets.repository.EquipmentRepository;
import com.millity.assets.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(3)
public class AssignmentDemoData implements CommandLineRunner {
    private final AssignmentRepository assignments;
    private final EquipmentRepository equipment;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;

    public AssignmentDemoData(AssignmentRepository assignments, EquipmentRepository equipment,
                              UserRepository users, AuditLogRepository auditLogs) {
        this.assignments=assignments; this.equipment=equipment; this.users=users; this.auditLogs=auditLogs;
    }

    @Override @Transactional
    public void run(String... args) {
        if (assignments.count() > 0) return;
        var actor = users.findAll().stream().findFirst().orElse(null);
        Equipment stock = equipment.findAll().stream().filter(item -> item.getQuantity() > 0).findFirst().orElse(null);
        if (actor == null || stock == null) return;
        stock.reduceQuantity(1);
        LocalDate assignmentDate = LocalDate.now().minusDays(3);
        Assignment assignment = assignments.save(new Assignment(stock.getBase(), stock.getType(), 1,
                "Demo Personnel", assignmentDate, false));
        String payload = "{\"assignmentId\":" + assignment.getId() + ",\"baseId\":" + stock.getBase().getId()
                + ",\"equipmentType\":\"" + stock.getType() + "\",\"quantity\":1,\"assignedToPersonnel\":\"Demo Personnel\",\"date\":\""
                + assignmentDate + "\",\"expended\":false}";
        auditLogs.save(new AuditLog(actor, "ASSIGNMENT_SEEDED", "/api/assignments/" + assignment.getId(), payload, Instant.now()));
    }
}
