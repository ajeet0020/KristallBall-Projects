package com.millity.assets.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="assignments")
public class Assignment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="base_id", nullable=false) private Base base;
    @Column(name="equipment_type", nullable=false, length=80) private String equipmentType;
    @Column(nullable=false) private Integer quantity;
    @Column(name="assigned_to_personnel", nullable=false, length=160) private String assignedToPersonnel;
    @Column(nullable=false) private LocalDate date;
    @Column(nullable=false) private Boolean expended;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected Assignment() {}
    public Assignment(Base base, String equipmentType, Integer quantity, String assignedToPersonnel, LocalDate date, Boolean expended) { this.base=base; this.equipmentType=equipmentType; this.quantity=quantity; this.assignedToPersonnel=assignedToPersonnel; this.date=date; this.expended=expended; this.createdAt=Instant.now(); }
    public Long getId(){return id;} public Base getBase(){return base;} public String getEquipmentType(){return equipmentType;} public Integer getQuantity(){return quantity;} public String getAssignedToPersonnel(){return assignedToPersonnel;} public LocalDate getDate(){return date;} public Boolean getExpended(){return expended;} public Instant getCreatedAt(){return createdAt;}
    public void markExpended() { this.expended=true; }
}
