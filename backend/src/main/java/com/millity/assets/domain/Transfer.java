package com.millity.assets.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="transfers")
public class Transfer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="from_base_id", nullable=false) private Base fromBase;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="to_base_id", nullable=false) private Base toBase;
    @Column(name="equipment_type", nullable=false, length=80) private String equipmentType;
    @Column(nullable=false) private Integer quantity;
    @Column(nullable=false) private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=24) private TransferStatus status;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="created_by", nullable=false) private User createdBy;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected Transfer() {}
    public Transfer(Base fromBase, Base toBase, String equipmentType, Integer quantity, LocalDate date, TransferStatus status, User createdBy) { this.fromBase=fromBase; this.toBase=toBase; this.equipmentType=equipmentType; this.quantity=quantity; this.date=date; this.status=status; this.createdBy=createdBy; this.createdAt=Instant.now(); }
    public Long getId(){return id;} public Base getFromBase(){return fromBase;} public Base getToBase(){return toBase;} public String getEquipmentType(){return equipmentType;} public Integer getQuantity(){return quantity;} public LocalDate getDate(){return date;} public TransferStatus getStatus(){return status;} public User getCreatedBy(){return createdBy;} public Instant getCreatedAt(){return createdAt;}
    public void setStatus(TransferStatus status) { this.status=status; }
}
