package com.millity.assets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false, length=120) private String action;
    @Column(nullable=false, length=300) private String endpoint;
    @Column(columnDefinition="text") private String payload;
    @Column(nullable=false) private Instant timestamp;
    protected AuditLog() {}
    public AuditLog(User user, String action, String endpoint, String payload, Instant timestamp) { this.user=user; this.action=action; this.endpoint=endpoint; this.payload=payload; this.timestamp=timestamp; }
    public Long getId(){return id;} public User getUser(){return user;} public String getAction(){return action;} public String getEndpoint(){return endpoint;} public String getPayload(){return payload;} public Instant getTimestamp(){return timestamp;}
}
