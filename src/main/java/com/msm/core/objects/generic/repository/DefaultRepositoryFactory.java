package com.msm.core.objects.generic.repository;

import com.msm.core.commons.Utils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

//@Component
@SuppressWarnings({"unchecked"})
public class DefaultRepositoryFactory implements RepositoryFactory {

    private final Map<Class<?>, JpaRepository<?, ?>> byEntity = new ConcurrentHashMap<>();
    private final Map<String, JpaRepository<?, ?>> byEntityName = new ConcurrentHashMap<>();
    private final Map<String, JpaRepository<?, ?>> byRepositoryName = new ConcurrentHashMap<>();

    public DefaultRepositoryFactory(List<JpaRepository<?, ?>> repositories, ListableBeanFactory beanFactory) {

        for (JpaRepository<?, ?> repo : repositories) {

            Class<?> domainType = resolveDomainType(repo);
            byEntity.put(domainType, repo);
            byEntityName.put(Utils.STR.lowCase(domainType.getSimpleName()), repo);
            String[] beanNames = beanFactory.getBeanNamesForType(repo.getClass());

            for (String beanName : beanNames) {
                byRepositoryName.put(Utils.STR.lowCase(beanName), repo);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <X, ID> JpaRepository<X, ID> getRepository(Class<X> entityClass) {
        JpaRepository<?, ?> repo = byEntity.get(entityClass);
        if (Objects.isNull(repo)) {
            throw new IllegalArgumentException("Repository not found for " + entityClass.getSimpleName());
        }

        return (JpaRepository<X, ID>) repo;
    }

    public <X> JpaRepository<X, ?> getRepository(String entityName) {
        JpaRepository<?, ?> repo = byEntityName.get(entityName);
        if (Objects.isNull(repo)) {
            throw new IllegalArgumentException("Repository not found for " + entityName);
        }

        return (JpaRepository<X, ?>) repo;
    }

    public <X> JpaRepository<X, ?> getRepositoryByRepositoryName(String repositoryName) {
        JpaRepository<?, ?> repo = byRepositoryName.get(repositoryName);
        if (Objects.isNull(repo)) {
            throw new IllegalArgumentException("Repository not found: " + repositoryName);
        }

        return (JpaRepository<X, ?>) repo;
    }

    private Class<?> resolveDomainType(JpaRepository<?, ?> repo) {
        ResolvableType type = ResolvableType.forClass(repo.getClass()).as(JpaRepository.class);
        return type.getGeneric(0).resolve();
    }
}