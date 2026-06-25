package com.msm.core.objects.service.imports.mapper;

import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.service.imports.Parsers;
import com.msm.core.objects.service.imports.RowMapperContext;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CsvRowMapper implements RowMapper<RowMapperContext, Map<String, Object>> {

    @Override
    public Map<String, Object> mapRow(RowMapperContext context) {
        Map<String, Object> dataRowMap = new LinkedHashMap<>();
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(context.getObjectName());
        objectMetadata.getAttributes().forEach(attribute -> {
            //Parsers
            String columnName = attribute.getColumnName();
//            String columnName = Utils.STR.lowCase(columnNameDb).toLowerCase();
            //record.isSet("email")

            if(context.getRow().isMapped(columnName)){
                try {
                    Object columnData = context.getRow().get(columnName);
                    if (hasRef(attribute)) {
                        dataRowMap.put(attribute.getFieldName(), columnData);
                    } else {
                        Object val;
                        if (Objects.nonNull(columnData) && attribute.isCollectionField()) {
                            val = Parsers.arrayParser(String.valueOf(columnData));
                        } else {
                            val = attribute.cast(columnData);
                        }
                        dataRowMap.put(attribute.getFieldName(), val);
                    }
//                    if(hasRef(attribute) && Objects.nonNull(columnData)) {
//                        String code = String.valueOf(columnData);
//                        if(Utils.STR.isNotBlank(code)) {
//                            context.getAttrCodeMap().compute(attribute, (attr, codes) ->  {
//                                if(Utils.CL.isEmpty(codes)) {
//                                    codes = new HashSet<>();
//                                }
//                                codes.add(String.valueOf(columnData));
//                                return codes;
//                            });
//                        }
//                    }
                } catch (Exception e) {
                    log.error("Error while reading column data for attribute {}", columnName, e);
                }
            }
        });

        return dataRowMap;
    }

    private boolean hasRef(Attribute attribute) {
        return Objects.nonNull(attribute.getAttributeRef()) && Utils.STR.isNotBlank(attribute.getAttributeRef().getFieldName());
    }
}
