package com.msm.core.objects.entity;

import com.msm.core.metadata.annotation.AttributeDefinition;
import com.msm.core.metadata.annotation.AttributeDefinitionRef;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public abstract class BusinessProcessEntity extends AuditingEntity {

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "workflow_instance_id")
    private UUID workflowInstanceId;

    @AttributeDefinition(attributeRef = @AttributeDefinitionRef(fieldName = "recordTypeIdReference", objectRef = "recordtypes"))
    @Column(name = "record_type_id")
    private UUID recordTypeId;

    @Column(name = "stage_ids")
    private List<UUID> stageIds;

    @Size(max = 60)
    @Column(name = "approval_status", length = 60)
    private String approvalStatus;

    @Column(name = "locked_edit")
    private Boolean lockedEdit;

    @Transient
    public boolean isLocked() {
        return Boolean.TRUE.equals(lockedEdit);
    }
}
