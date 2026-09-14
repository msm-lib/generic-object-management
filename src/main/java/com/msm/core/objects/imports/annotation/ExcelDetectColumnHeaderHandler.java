package com.msm.core.objects.imports.annotation;

import com.msm.core.action.annotations.ExtendContextKey;
import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.condition.AlwaysTrueCondition;
import com.msm.core.commons.Condition;
import com.msm.core.commons.Constants;
import com.msm.core.objects.imports.ImportActionNamed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Handler(action = ImportActionNamed.Excel.DETECT_COLUMN_HEADER_MAPPING)
public @interface ExcelDetectColumnHeaderHandler {
    String resource() default Constants.GENERIC_RESOURCE_NAME;
    ExtendContextKey[] keyContexts() default {};
    Class<? extends Condition> condition() default AlwaysTrueCondition.class;
}
