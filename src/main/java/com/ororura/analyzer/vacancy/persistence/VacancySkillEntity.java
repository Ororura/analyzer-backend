package com.ororura.analyzer.vacancy.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "vacancy_skills", uniqueConstraints = @UniqueConstraint(
        name = "uk_vacancy_skills_vacancy_normalized", columnNames = {"vacancy_id", "normalized_skill"}))
public class VacancySkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vacancy_id")
    private VacancyEntity vacancy;
    @Column(nullable = false, length = 255)
    private String skill;
    @Column(name = "normalized_skill", nullable = false, length = 255)
    private String normalizedSkill;

    protected VacancySkillEntity() {
    }

    VacancySkillEntity(VacancyEntity vacancy, String skill, String normalizedSkill) {
        this.vacancy = vacancy;
        this.skill = skill;
        this.normalizedSkill = normalizedSkill;
    }

    public String skill() { return skill; }
    public String normalizedSkill() { return normalizedSkill; }
    void rename(String value) { skill = value; }
}
