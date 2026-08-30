package com.ororura.analyzer.vacancy.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VacancyRepository extends JpaRepository<VacancyEntity, Long>, JpaSpecificationExecutor<VacancyEntity> {

    Optional<VacancyEntity> findBySourceAndExternalId(String source, String externalId);

    List<VacancyEntity> findBySourceAndExternalIdIn(String source, List<String> externalIds);
}
