package com.millity.assets.api;

import com.millity.assets.domain.TransferStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TransferResponse(Long id, Long fromBaseId, String fromBaseName, Long toBaseId, String toBaseName,
                               String equipmentType, Integer quantity, LocalDate date, TransferStatus status,
                               Instant createdAt, Long createdById, String createdBy) {}
