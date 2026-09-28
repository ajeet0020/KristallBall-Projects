package com.millity.assets.api;

import java.time.LocalDate;

public record DashboardResponse(LocalDate fromDate, LocalDate toDate, Long baseId, String equipmentType,
                                long openingBalance, long closingBalance, long netMovement,
                                long purchases, long transferIn, long transferOut,
                                long assigned, long expended) {}
