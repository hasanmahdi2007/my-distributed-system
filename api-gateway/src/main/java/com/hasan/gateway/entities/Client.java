package com.hasan.gateway.entities;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client {
    
    @Id
    @GeneratedValue 
    private UUID id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "tier_type", nullable = false)
    private String tierType;

    @Column(name = "created_at", updatable = false, nullable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTierType() { return tierType; }
    public void setTierType(String tierType) { this.tierType = tierType; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
}