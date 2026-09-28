package com.millity.assets.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "bases")
public class Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 120) private String name;
    @Column(nullable = false, length = 200) private String location;
    protected Base() {}
    public Base(String name, String location) { this.name = name; this.location = location; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
}
