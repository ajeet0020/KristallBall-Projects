package com.millity.assets.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 80) private String username;
    @Column(name = "password_hash", nullable = false, length = 100) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Role role;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_base_id") private Base assignedBase;
    protected User() {}
    public User(String name, String username, String password, Role role, Base assignedBase) { this.name=name; this.username=username; this.password=password; this.role=role; this.assignedBase=assignedBase; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public Base getAssignedBase() { return assignedBase; }
}
