package com.millity.assets.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name = "purchases")
public class Purchase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="base_id", nullable=false) private Base base;
    @Column(name="equipment_type", nullable=false, length=80) private String equipmentType;
    @Column(nullable=false) private Integer quantity;
    @Column(nullable=false) private LocalDate date;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="created_by", nullable=false) private User createdBy;
    protected Purchase() {}
    public Purchase(Base base, String equipmentType, Integer quantity, LocalDate date, User createdBy) { this.base=base; this.equipmentType=equipmentType; this.quantity=quantity; this.date=date; this.createdBy=createdBy; }
    public Long getId(){return id;} public Base getBase(){return base;} public String getEquipmentType(){return equipmentType;} public Integer getQuantity(){return quantity;} public LocalDate getDate(){return date;} public User getCreatedBy(){return createdBy;}
    public void setDetails(Base base, String equipmentType, Integer quantity, LocalDate date) { this.base=base; this.equipmentType=equipmentType; this.quantity=quantity; this.date=date; }
}
