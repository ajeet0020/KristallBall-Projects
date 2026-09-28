package com.millity.assets.api;

import com.millity.assets.domain.EquipmentStatus;

public record EquipmentOption(Long id, Long baseId, String type, String name, Integer availableQuantity,
                              EquipmentStatus status) {}
