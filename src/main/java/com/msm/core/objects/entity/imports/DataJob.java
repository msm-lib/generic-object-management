package com.msm.core.objects.entity.imports;

import com.msm.core.metadata.annotation.AttributeDefinition;
import com.msm.core.objects.entity.AuditingEntity;
import com.msm.core.security.annotations.IgnorePermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@IgnorePermission
@Getter
@Setter
@Entity
@Table(name = "data_job")
public class DataJob extends AuditingEntity {

    @Id
    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Size(max = 60)
    @Column(name = "job_type")
    private String jobType; // 'IMPORT' or 'EXPORT'

    @NotNull
    @Size(max = 60)
    @Column(name = "object_name")
    private String objectName; // 'PRODUCT', 'USER', 'ORDER'...

    @NotNull
    @Size(max = 60)
    @Column(name = "status")
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    @Size(max = 1000)
    @Column(name = "file_name", length = 1000)
    private String fileName;

    @NotNull
    @AttributeDefinition(defaultValue = "0", required = true)
    @Column(name = "progress")
    private Integer progress;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
