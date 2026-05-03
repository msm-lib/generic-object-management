package com.msm.core.objects.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryFactory {
    <X, ID> JpaRepository<X, ID> getRepository(Class<X> entityClass);
    <X> JpaRepository<X, ?> getRepository(String entityName);
    <X> JpaRepository<X, ?> getRepositoryByRepositoryName(String repositoryName);
}