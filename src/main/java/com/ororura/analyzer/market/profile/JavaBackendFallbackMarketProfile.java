package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.List;
import java.util.Map;

import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.profile.AnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import org.springframework.stereotype.Component;

/** Last-resort bootstrap data. Frequencies are explicitly fallback defaults, not live market statistics. */
@Component
public class JavaBackendFallbackMarketProfile implements FallbackMarketProfileDefinition {
    private final AnalysisTechnologyCatalog catalog;

    public JavaBackendFallbackMarketProfile(AnalysisTechnologyCatalog catalog) { this.catalog = catalog; }

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.JAVA_BACKEND; }

    @Override public List<AnalysisCriterion> criteria() {
        return List.of(criterion("java-fallback", "Java", .20), criterion("spring-backend-fallback", "Spring Backend", .20),
                criterion("backend-architecture-fallback", "Backend Architecture", .20),
                criterion("persistence-fallback", "Databases & Persistence", .15),
                criterion("infrastructure-fallback", "Infrastructure", .15), criterion("testing-fallback", "Testing", .10));
    }

    @Override public List<MarketRequirement> requirements() {
        Map<String, Double> frequencies = Map.ofEntries(Map.entry("Java", .93), Map.entry("Spring Boot", .89),
                Map.entry("REST API", .82), Map.entry("SQL", .81), Map.entry("PostgreSQL", .68),
                Map.entry("Hibernate/JPA", .61), Map.entry("Docker", .54), Map.entry("Testing", .52),
                Map.entry("Kafka", .41), Map.entry("Redis", .27), Map.entry("Kubernetes", .19));
        return catalog.technologies(profile()).stream().map(value -> new MarketRequirement(
                MarketRequirementId.of(RequirementType.TECHNOLOGY, value.name()), value.name(),
                RequirementType.TECHNOLOGY, frequencies.getOrDefault(value.name(), 0.0))).toList();
    }

    private static AnalysisCriterion criterion(String id, String label, double weight) {
        return new AnalysisCriterion(id, label, "Assessment for " + label, weight, 0);
    }
}
