package com.millity.assets.api;

import com.millity.assets.domain.TransferStatus;
import jakarta.validation.constraints.NotNull;

public record TransferStatusRequest(@NotNull TransferStatus status) {}
