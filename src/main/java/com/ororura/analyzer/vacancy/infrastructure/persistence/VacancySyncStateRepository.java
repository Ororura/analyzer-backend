package com.ororura.analyzer.vacancy.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VacancySyncStateRepository extends JpaRepository<VacancySyncStateEntity, String> {
}
