package com.millity.assets.config;

import com.millity.assets.domain.AuditLog;
import com.millity.assets.domain.Purchase;
import com.millity.assets.repository.AuditLogRepository;
import com.millity.assets.repository.BaseRepository;
import com.millity.assets.repository.PurchaseRepository;
import com.millity.assets.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class PurchaseDemoData implements CommandLineRunner {
    private final PurchaseRepository purchases;
    private final BaseRepository bases;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;

    public PurchaseDemoData(PurchaseRepository purchases, BaseRepository bases, UserRepository users, AuditLogRepository auditLogs) {
        this.purchases=purchases; this.bases=bases; this.users=users; this.auditLogs=auditLogs;
    }

    @Override public void run(String... args) {
        if (purchases.count() > 0) return;
        var user = users.findAll().stream().findFirst().orElse(null);
        var base = bases.findAll().stream().findFirst().orElse(null);
        if (user == null || base == null) return;
        seed(user, base, "VEHICLE", 3, LocalDate.now().minusDays(7));
        seed(user, base, "AMMUNITION", 240, LocalDate.now().minusDays(2));
    }

    private void seed(com.millity.assets.domain.User user, com.millity.assets.domain.Base base,
                      String equipmentType, int quantity, LocalDate date) {
        Purchase purchase = purchases.save(new Purchase(base, equipmentType, quantity, date, user));
        String payload = "{\"purchaseId\":" + purchase.getId() + ",\"baseId\":" + base.getId()
                + ",\"equipmentType\":\"" + equipmentType + "\",\"quantity\":" + quantity
                + ",\"date\":\"" + date + "\"}";
        auditLogs.save(new AuditLog(user, "PURCHASE_SEEDED", "/api/purchases/" + purchase.getId(), payload, Instant.now()));
    }
}
