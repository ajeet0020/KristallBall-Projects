package com.millity.assets.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment")
public class Equipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String type;
    @Column(nullable = false, length = 160) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "base_id", nullable = false) private Base base;
    @Column(nullable = false) private Integer quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private EquipmentStatus status;
    protected Equipment() {}
    public Equipment(String type, String name, Base base, Integer quantity, EquipmentStatus status) { this.type=type; this.name=name; this.base=base; this.quantity=quantity; this.status=status; }
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public Base getBase() { return base; }
    public Integer getQuantity() { return quantity; }
    public EquipmentStatus getStatus() { return status; }
    public void reduceQuantity(int amount) { this.quantity -= amount; }
}
