package com.msm.core.objects.service.imports.mapper;

@FunctionalInterface
public interface RowMapper<F, T> {
    T mapRow(F row);
}