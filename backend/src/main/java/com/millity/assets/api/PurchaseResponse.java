package com.millity.assets.api;

import java.time.LocalDate;

public record PurchaseResponse(Long id, Long baseId, String baseName, String equipmentType,
                               Integer quantity, LocalDate date, Long createdById, String createdBy) {}
