package com.ororura.analyzer.vacancy.persistence;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@DynamicUpdate
@Table(name = "vacancies", uniqueConstraints = @UniqueConstraint(
        name = "uk_vacancies_source_external", columnNames = {"source", "external_id"}))
public class VacancyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;
    @Column(nullable = false, length = 64)
    private String source;
    @Column(nullable = false, length = 500)
    private String title;
    @Column(length = 500)
    private String company;
    @Column(name = "company_id", length = 128)
    private String companyId;
    @Column(columnDefinition = "text")
    private String description;
    private String experience;
    private String employment;
    private String schedule;
    @Column(name = "work_format", length = 32)
    private String workFormat;
    @Column(name = "salary_from")
    private Integer salaryFrom;
    @Column(name = "salary_to")
    private Integer salaryTo;
    @Column(name = "salary_currency", length = 16)
    private String salaryCurrency;
    @Column(name = "salary_gross")
    private Boolean salaryGross;
    @Column(length = 500)
    private String location;
    @Column(columnDefinition = "text")
    private String url;
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    @Column(name = "source_created_at")
    private OffsetDateTime sourceCreatedAt;
    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;
    @Column(name = "first_seen_at", nullable = false)
    private OffsetDateTime firstSeenAt;
    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;
    @Column(name = "last_checked_at", nullable = false)
    private OffsetDateTime lastCheckedAt;
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawPayload;

    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VacancySkillEntity> skills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vacancy_requirements", joinColumns = @JoinColumn(name = "vacancy_id"))
    @OrderColumn(name = "list_order")
    @Column(name = "requirement", columnDefinition = "text")
    private List<String> requirements = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vacancy_responsibilities", joinColumns = @JoinColumn(name = "vacancy_id"))
    @OrderColumn(name = "list_order")
    @Column(name = "responsibility", columnDefinition = "text")
    private List<String> responsibilities = new ArrayList<>();

    protected VacancyEntity() {
    }

    public static VacancyEntity create(String source, String externalId, OffsetDateTime now) {
        VacancyEntity entity = new VacancyEntity();
        entity.source = source;
        entity.externalId = externalId;
        entity.firstSeenAt = now;
        entity.lastSeenAt = now;
        entity.lastCheckedAt = now;
        entity.active = true;
        return entity;
    }

    public void replaceContent(VacancyContent content, String hash, Map<String, Object> rawPayload, OffsetDateTime now) {
        title = content.title();
        company = content.company();
        companyId = content.companyId();
        description = content.description();
        experience = content.experience();
        employment = content.employment();
        schedule = content.schedule();
        workFormat = content.workFormat();
        salaryFrom = content.salaryFrom();
        salaryTo = content.salaryTo();
        salaryCurrency = content.salaryCurrency();
        salaryGross = content.salaryGross();
        location = content.location();
        url = content.url();
        publishedAt = content.publishedAt();
        sourceCreatedAt = content.sourceCreatedAt();
        sourceUpdatedAt = content.sourceUpdatedAt();
        requirements.clear();
        requirements.addAll(content.requirements());
        responsibilities.clear();
        responsibilities.addAll(content.responsibilities());
        Map<String, String> uniqueSkills = new LinkedHashMap<>();
        content.skills().forEach(skill -> uniqueSkills.putIfAbsent(skill.normalized(), skill.value()));
        Map<String, VacancySkillEntity> existingSkills = skills.stream().collect(java.util.stream.Collectors.toMap(
                VacancySkillEntity::normalizedSkill, skill -> skill));
        skills.removeIf(skill -> !uniqueSkills.containsKey(skill.normalizedSkill()));
        uniqueSkills.forEach((normalized, value) -> {
            VacancySkillEntity existing = existingSkills.get(normalized);
            if (existing == null) skills.add(new VacancySkillEntity(this, value, normalized));
            else existing.rename(value);
        });
        contentHash = hash;
        this.rawPayload = rawPayload;
        markSeen(now);
    }

    public void markSeen(OffsetDateTime now) {
        lastSeenAt = now;
        lastCheckedAt = now;
        active = true;
        archivedAt = null;
    }

    public void archive(OffsetDateTime now) {
        active = false;
        archivedAt = now;
        lastCheckedAt = now;
    }

    public Long id() { return id; }
    public String externalId() { return externalId; }
    public String source() { return source; }
    public String title() { return title; }
    public String company() { return company; }
    public String companyId() { return companyId; }
    public String description() { return description; }
    public String experience() { return experience; }
    public String employment() { return employment; }
    public String schedule() { return schedule; }
    public String workFormat() { return workFormat; }
    public Integer salaryFrom() { return salaryFrom; }
    public Integer salaryTo() { return salaryTo; }
    public String salaryCurrency() { return salaryCurrency; }
    public Boolean salaryGross() { return salaryGross; }
    public String location() { return location; }
    public String url() { return url; }
    public OffsetDateTime publishedAt() { return publishedAt; }
    public OffsetDateTime firstSeenAt() { return firstSeenAt; }
    public OffsetDateTime lastSeenAt() { return lastSeenAt; }
    public OffsetDateTime lastCheckedAt() { return lastCheckedAt; }
    public String contentHash() { return contentHash; }
    public boolean active() { return active; }
    public OffsetDateTime archivedAt() { return archivedAt; }
    public List<VacancySkillEntity> skills() { return List.copyOf(skills); }
    public List<String> requirements() { return List.copyOf(requirements); }
    public List<String> responsibilities() { return List.copyOf(responsibilities); }
}
