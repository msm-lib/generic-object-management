package com.msm.core.objects.service;

import com.msm.core.commons.Utils;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.filter.cache.EntityMetadataFactory;
import com.msm.core.filter.domain.FieldMetadata;
import com.msm.core.filter.domain.FilterCondition;
import com.msm.core.filter.domain.FilterGroup;
import com.msm.core.filter.domain.FilterObject;
import com.msm.core.filter.domain.FilterOperator;
import com.msm.core.filter.domain.LogicalOperator;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.objects.ObjectConstants;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class DefaultSoftDeleteFilter {

    private final EntityClassFactory entityClassFactory;

    public FilterGroup defaultFilterGroup() {
        FilterGroup filterGroup = new FilterGroup();
        filterGroup.setOperator(LogicalOperator.OR);
        FilterCondition filterCondition = FilterCondition.create(ObjectConstants.IS_DELETED_FIELD, FilterOperator.EQUALS, Boolean.FALSE);
        FilterCondition filterCondition0 = FilterCondition.create(ObjectConstants.IS_DELETED_FIELD, FilterOperator.EQUALS, null);
        filterGroup.setConditions(Utils.CL.newArrayList(filterCondition, filterCondition0));

        return filterGroup;
    }

    private boolean isDeletedFilter(FilterGroup filterGroup) {
        if (Objects.isNull(filterGroup)) {
            return false;
        }
        for (FilterObject filter : Utils.CL.emptyIfNull(filterGroup.getConditions())) {
            if (filter instanceof FilterCondition condition) {
                if (Utils.STR.equalIgnoreCase(condition.getField(), ObjectConstants.IS_DELETED_FIELD)) {
                    return true;
                }
            } else if (filter instanceof FilterGroup group && isDeletedFilter(group)) {
                return true;
            }
        }

        return false;
    }

    public void addDefaultFilter(ObjectFilterRequest filter) {
        EntityType<?> entityType = entityClassFactory.getEntityType(filter.getObjectInfo().getName());
        FieldMetadata fieldMetadata = EntityMetadataFactory.get(entityType.getJavaType(), ObjectConstants.IS_DELETED_FIELD);
        if (Objects.isNull(fieldMetadata)) {
            return;
        }
        FilterGroup currentFilter = filter.getFilters();

        if (Objects.isNull(currentFilter)) {
            filter.setFilters(FilterGroup.builder()
                    .operator(LogicalOperator.AND)
                    .conditions(Utils.CL.newArrayList(defaultFilterGroup()))
                    .build()
            );
            return;
        }

        if (!isDeletedFilter(currentFilter)) {
            if(Utils.CL.isEmpty(currentFilter.getConditions())) {
                currentFilter.setConditions(Utils.CL.newArrayList(defaultFilterGroup()));
            } else {
                currentFilter.getConditions().add(defaultFilterGroup()); // no extra AND wrapper
            }
        }
    }
}
