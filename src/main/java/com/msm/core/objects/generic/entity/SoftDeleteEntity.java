package com.msm.core.objects.generic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public abstract class SoftDeleteEntity extends BusinessProcessEntity{
    @Column(name = "deleted_by")
    @Size(max = 300)
    private String deletedBy;

    @Column(name = "deleted_by_id")
    private UUID deletedById;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = Boolean.FALSE;

    public void softDelete(String username, UUID userId) {
        this.deletedAt = Instant.now();
        this.deletedBy = username;
        this.deletedById = userId;
        this.isDeleted = true;
    }
}
