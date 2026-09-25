package com.msm.core.objects.entity.imports;

import com.msm.core.metadata.annotation.AttributeDefinition;
import com.msm.core.objects.entity.SoftDeleteEntity;
import com.msm.core.security.annotations.IgnorePermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_job")
public class ImportJob extends SoftDeleteEntity {

    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "object_name")
    private String objectName;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "status")
    private String status;

    @AttributeDefinition(defaultValue = "0", required = true)
    @Column(name = "total_rows")
    private Long totalRows;

    @AttributeDefinition(defaultValue = "0", required = true)
    @Column(name = "valid_rows")
    private Long validRows;

    @AttributeDefinition(defaultValue = "0", required = true)
    @Column(name = "success_rows")
    private Long successRows;

    @AttributeDefinition(defaultValue = "0", required = true)
    @Column(name = "error_rows")
    private Long errorRows;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_file_path")
    private String errorFilePath;

    @Column(name = "error_message")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_info")
    private Map<String, Object> fileInfo;


    @Column(name = "custom_values")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> customValues = new HashMap<>();
}
