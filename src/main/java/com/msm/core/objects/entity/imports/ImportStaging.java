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

import java.util.Map;
import java.util.UUID;

@IgnorePermission
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_staging")
public class ImportStaging extends SoftDeleteEntity {

    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @AttributeDefinition(required = true)
    @Column(name = "import_id")
    private UUID importId;

    @AttributeDefinition(required = true)
    @Column(name = "row_number")
    private Long rowNumber;

    @AttributeDefinition(required = true)
    @Column(name = "identity_key")
    private String identityKey;

    @AttributeDefinition(required = true)
    @Column(name = "identity_level")
    private int identityLevel;

    @AttributeDefinition(required = true)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false)
    private Map<String, Object> data;
}
