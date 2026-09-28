package com.millity.assets.api;

import java.time.Instant;
import java.time.LocalDate;

public record AssignmentResponse(Long id, Long baseId, String baseName, String equipmentType,
                                 Integer quantity, String assignedToPersonnel, LocalDate date,
                                 Boolean expended, Instant createdAt) {}
