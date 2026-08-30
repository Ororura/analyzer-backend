package com.ororura.analyzer.vacancy.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VacancySyncStateRepository extends JpaRepository<VacancySyncStateEntity, String> {
}
