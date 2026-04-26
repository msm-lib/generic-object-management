package com.msm.core.objects.generic.service;

import com.msm.core.commons.Utils;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.filter.cache.EntityMetadataFactory;
import com.msm.core.filter.domain.*;
import com.msm.core.objects.generic.ObjectConstants;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
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
