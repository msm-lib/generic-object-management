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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@IgnorePermission
@Getter
@Setter
@Entity
@Table(name = "export_job")
public class ExportJob extends AuditingEntity {

    @Id
    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Size(max = 60)
    @Column(name = "object_name")
    private String objectName;

    @Column(name = "attachment_id")
    private UUID attachmentId;

    @NotNull
    @Size(max = 60)
    @Column(name = "status")
    private String status;

    @Column(name = "filter_criteria")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> filterCriteria = new HashMap<>();

    @Size(max = 1000)
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path", columnDefinition = "text")
    private String filePath;

    @NotNull
    @AttributeDefinition(defaultValue = "0", required = true)
    @Column(name = "total_rows")
    private Long totalRows;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}

