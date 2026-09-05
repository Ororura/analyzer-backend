package com.ororura.analyzer.analysis.persistence;

import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics.*;
import com.ororura.analyzer.vacancy.requirement.generation.GeneratedCriterionGroup;
import com.ororura.analyzer.resume.market.RequirementType;

@Entity @Immutable @Table(name="market_snapshots")
public class MarketSnapshotEntity {
    @Id private UUID id;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private Map<String,String> provenance;
    @Column(nullable=false) private Instant generatedAt;
    @Column(nullable=false,length=64) private String pipelineVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private MarketSegment segment;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<String> queries;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<MarketSnapshot.SnapshotVacancy> vacancies;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<VacancyRequirements> vacancyRequirements;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<String> warnings;
    @Column(nullable=false) private int scannedCount;
    @Column(nullable=false) private int uniqueCount;
    @Column(nullable=false) private boolean truncated;
    @Column(nullable=false) private int fetchedCount;
    @Column(nullable=false) private int processedCount;
    @Column(nullable=false) private int failedCount;
    @Column(nullable=false) private boolean sufficientSample;
    @ElementCollection @CollectionTable(name="market_snapshot_requirements",joinColumns=@JoinColumn(name="snapshot_id"))
    @OrderColumn(name="position") private List<RequirementRow> requirements=new ArrayList<>();
    @ElementCollection @CollectionTable(name="market_snapshot_criteria",joinColumns=@JoinColumn(name="snapshot_id"))
    @OrderColumn(name="position") private List<CriterionRow> criteria=new ArrayList<>();
    protected MarketSnapshotEntity() {}
    public MarketSnapshotEntity(MarketSnapshot s) {
        id=s.id();provenance=s.provenance();generatedAt=s.generatedAt();pipelineVersion=s.pipelineVersion();segment=s.segment();queries=s.queries();
        vacancies=s.vacancies();warnings=s.warnings();scannedCount=s.scannedCount();uniqueCount=s.uniqueCount();truncated=s.truncated();
        var stats=s.statistics();fetchedCount=stats.fetchedCount();processedCount=stats.processedCount();failedCount=stats.failedCount();
        sufficientSample=stats.sufficientSample();vacancyRequirements=stats.vacancyRequirements();
        requirements=s.statistics().requirements().stream().map(RequirementRow::new).toList();
        criteria=s.criteria().stream().map(CriterionRow::new).toList();
    }
    public UUID id() { return id; }
    public MarketSnapshot domain() {
        var stats=new MarketRequirementStatistics(null,fetchedCount,processedCount,processedCount,failedCount,
                sufficientSample,vacancyRequirements,requirements.stream().map(RequirementRow::domain).toList());
        return new MarketSnapshot(id,segment,generatedAt,pipelineVersion,provenance,queries,scannedCount,uniqueCount,truncated,
                vacancies,stats,criteria.stream().map(CriterionRow::domain).toList(),warnings);
    }
    @Embeddable
    public static class RequirementRow {
        @Column(nullable=false,length=256) private String requirementId;
        @Column(nullable=false,length=500) private String label;
        @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private RequirementType type;
        @Column(nullable=false) private int totalVacancyCount;
        @Column(nullable=false) private int requiredCount;
        @Column(nullable=false) private int preferredCount;
        @Column(nullable=false) private int optionalCount;
        @Column(nullable=false) private double frequency;
        @Column(nullable=false) private double requiredFrequency;
        @Column(nullable=false) private double preferredFrequency;
        @Column(nullable=false) private double optionalFrequency;
        protected RequirementRow() {}
        RequirementRow(RequirementStatistics s) {
            requirementId=s.id();label=s.label();type=s.type();totalVacancyCount=s.totalVacancyCount();
            requiredCount=s.requiredCount();preferredCount=s.preferredCount();optionalCount=s.optionalCount();
            frequency=s.frequency();requiredFrequency=s.requiredFrequency();preferredFrequency=s.preferredFrequency();optionalFrequency=s.optionalFrequency();
        }
        RequirementStatistics domain() { return new RequirementStatistics(requirementId,label,type,totalVacancyCount,
                requiredCount,preferredCount,optionalCount,frequency,requiredFrequency,preferredFrequency,optionalFrequency); }
    }
    @Embeddable
    public static class CriterionRow {
        @Column(nullable=false,length=500) private String label;
        @Column(nullable=false,columnDefinition="text") private String description;
        @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<String> requirementIds;
        protected CriterionRow() {}
        CriterionRow(GeneratedCriterionGroup g) { label=g.label();description=g.description();requirementIds=g.requirementIds(); }
        GeneratedCriterionGroup domain() { return new GeneratedCriterionGroup(label,description,requirementIds); }
    }
}
