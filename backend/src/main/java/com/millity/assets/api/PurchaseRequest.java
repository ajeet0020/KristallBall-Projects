package com.millity.assets.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PurchaseRequest(
        Long baseId,
        @NotBlank @Size(max = 80) String equipmentType,
        @NotNull @Positive Integer quantity,
        @NotNull LocalDate date) {}
