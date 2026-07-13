package com.hasan.gateway.entities;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "api_keys", 
    indexes = {
        @Index(name = "idx_api_keys_hash", columnList = "key_hash") 
    }
)
public class ApiKey {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "request_limit", nullable = false)
    private Integer requestLimit;

    @Column(name = "current_usage")
    private Integer currentUsage = 0;

    // 2. OPTIMISTIC LOCKING: The most critical addition for a distributed gateway
    @Version 
    private Long version;

    // 3. SOFT DELETES: Never permanently delete audit data
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; 

    @Column(name = "last_accessed_minute")
    private ZonedDateTime lastAccessedMinute = ZonedDateTime.now();

    @Column(name = "created_at", updatable = false, nullable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }

    // --- GETTERS AND SETTERS ---
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public Integer getRequestLimit() { return requestLimit; }
    public void setRequestLimit(Integer requestLimit) { this.requestLimit = requestLimit; }

    public Integer getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(Integer currentUsage) { this.currentUsage = currentUsage; }

    public Long getVersion() { return version; }
    // No setter for version! Hibernate handles this automatically.

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getLastAccessedMinute() { return lastAccessedMinute; }
    public void setLastAccessedMinute(ZonedDateTime lastAccessedMinute) { this.lastAccessedMinute = lastAccessedMinute; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
}