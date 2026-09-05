package com.ororura.analyzer.analysis.persistence;

import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

@Entity
@Table(name="analysis_profiles")
public class AnalysisProfileEntity {
    @Id private UUID id;
    @Column(nullable=false) private UUID ownerId;
    @Version private Long version;
    @Column(nullable=false,length=120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private CareerDirection direction;
    @Column(nullable=false,length=80) private String specialization;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private CandidateGrade targetGrade;
    @Enumerated(EnumType.STRING) @Column(length=32) private ResumeAnalysisProfile preset;
    @ElementCollection @CollectionTable(name="analysis_profile_technologies", joinColumns=@JoinColumn(name="profile_id"))
    @Column(name="technology",nullable=false,length=80) @OrderColumn(name="position")
    private List<String> technologies = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private MarketFilters marketFilters;
    @Column(nullable=false,length=64) private String scoringPolicyVersion;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    private Instant deletedAt;
    protected AnalysisProfileEntity() {}
    public AnalysisProfileEntity(UUID owner, UserAnalysisProfile value) {
        id=value.id(); ownerId=owner; createdAt=value.createdAt(); update(value);
    }
    public void update(UserAnalysisProfile v) {
        name=v.name(); direction=v.direction(); specialization=v.specialization(); targetGrade=v.targetGrade();
        preset=v.preset(); technologies.clear(); technologies.addAll(v.technologies()); marketFilters=v.marketFilters();
        scoringPolicyVersion=v.scoringPolicyVersion(); updatedAt=v.updatedAt();
    }
    public void delete(Instant now) { deletedAt=now; updatedAt=now; }
    public UserAnalysisProfile domain() {
        return new UserAnalysisProfile(id,version == null ? 0 : version,name,direction,specialization,targetGrade,technologies,
                marketFilters,preset,scoringPolicyVersion,createdAt,updatedAt);
    }
}
