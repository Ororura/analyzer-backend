package com.ororura.analyzer.vacancy.infrastructure.persistence;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VacancyContentHasherTests {
    private final VacancyContentHasher hasher = new VacancyContentHasher();

    @Test
    void hashIsStableForSkillOrderAndIgnoresSyncMetadata() {
        VacancyContent first = content(List.of(new VacancyContent.Skill("Java", "java"),
                new VacancyContent.Skill("Spring Boot", "spring boot")));
        VacancyContent reordered = content(List.of(new VacancyContent.Skill("Spring Boot", "spring boot"),
                new VacancyContent.Skill("Java", "java")));

        assertThat(hasher.hash(first)).isEqualTo(hasher.hash(reordered));
    }

    @Test
    void meaningfulChangeChangesHash() {
        VacancyContent first = content(List.of(new VacancyContent.Skill("Java", "java")));
        VacancyContent changed = new VacancyContent("Senior Java Developer", first.company(), first.companyId(),
                first.description(), first.skills(), first.requirements(), first.responsibilities(), first.experience(),
                first.employment(), first.schedule(), first.workFormat(), first.salaryFrom(), first.salaryTo(),
                first.salaryCurrency(), first.salaryGross(), first.location(), first.url(), first.publishedAt(), null, null);
        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(changed));
    }

    private static VacancyContent content(List<VacancyContent.Skill> skills) {
        return new VacancyContent("Java Developer", "Acme", "1", "Build services", skills,
                List.of("Java"), List.of("Build"), "1-3", "FULL", "REMOTE", "REMOTE",
                100_000, 150_000, "RUR", true, "Moscow", "https://example.test/1", null, null, null);
    }
}
