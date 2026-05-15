package com.msm.core.objects.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Data
@MappedSuperclass
public abstract class AuditingEntity {

    @Size(max = 300)
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_by_id")
    private UUID createdById;

    @Column(name = "created_at")
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_by")
    @Size(max = 300)
    private String updatedBy;

    @Column(name = "updated_by_id")
    private UUID updatedById;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    public void createdAuditingEntity(UUID userId, String username) {
        this.createdById = userId;
        this.createdBy = username;
        this.createdAt = Instant.now();
    }

    public void updatedAuditingEntity(UUID userId, String username) {
        this.updatedById = userId;
        this.updatedBy = username;
        this.updatedAt = Instant.now();
    }
}