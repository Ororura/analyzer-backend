package com.ororura.analyzer.vacancy.requirement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics.RequirementStatistics;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics.MappedRequirement;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics.VacancyRequirements;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
class VacancyRequirementAggregator {
    private static final Logger log = LoggerFactory.getLogger(VacancyRequirementAggregator.class);

    private final MarketRequirementProperties properties;

    VacancyRequirementAggregator(MarketRequirementProperties properties) {
        this.properties = properties;
    }

    MarketRequirementStatistics aggregate(ResumeAnalysisProfile profile, int fetchedCount,
            List<CanonicalVacancyRequirements> processedVacancies) {
        int processedCount = processedVacancies.size();
        Map<String, MutableCounts> counts = new LinkedHashMap<>();
        for (CanonicalVacancyRequirements vacancy : processedVacancies) {
            Map<String, CanonicalRequirement> unique = new LinkedHashMap<>();
            for (CanonicalRequirement requirement : vacancy.requirements()) {
                unique.merge(requirement.id(), requirement, VacancyRequirementAggregator::stronger);
            }
            for (CanonicalRequirement requirement : unique.values()) {
                counts.computeIfAbsent(requirement.id(), ignored -> new MutableCounts(requirement))
                        .increment(requirement.importance());
            }
        }

        List<RequirementStatistics> requirements = new ArrayList<>();
        if (processedCount > 0) {
            counts.values().stream()
                    .filter(value -> value.total() >= properties.minVacancyCount())
                    .filter(value -> frequency(value.total(), processedCount)
                            >= properties.minRequirementFrequency())
                    .map(value -> value.toStatistics(processedCount))
                    .sorted(Comparator.comparingInt(RequirementStatistics::totalVacancyCount).reversed()
                            .thenComparing(RequirementStatistics::id))
                    .forEach(requirements::add);
        }
        java.util.Set<String> retainedIds = requirements.stream()
                .map(RequirementStatistics::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<VacancyRequirements> mappings = processedVacancies.stream()
                .map(vacancy -> new VacancyRequirements(vacancy.vacancyId(), vacancy.requirements().stream()
                        .filter(requirement -> retainedIds.contains(requirement.id()))
                        .collect(java.util.stream.Collectors.toMap(CanonicalRequirement::id,
                                requirement -> new MappedRequirement(requirement.id(), requirement.importance()),
                                VacancyRequirementAggregator::strongerMapping, LinkedHashMap::new))
                        .values().stream().toList()))
                .toList();
        int failedCount = fetchedCount - processedCount;
        log.info("Vacancy requirements aggregated profile={} fetched={} processed={} failed={} uniqueRequirements={} requirementsAfterFiltering={}",
                profile, fetchedCount, processedCount, failedCount, counts.size(), requirements.size());
        return new MarketRequirementStatistics(profile, fetchedCount, processedCount, processedCount,
                failedCount, processedCount >= properties.minimumSampleSize(), mappings, requirements);
    }

    private static CanonicalRequirement stronger(CanonicalRequirement first, CanonicalRequirement second) {
        return rank(second.importance()) > rank(first.importance()) ? second : first;
    }

    private static int rank(RequirementImportance importance) {
        return switch (importance) {
            case REQUIRED -> 3;
            case PREFERRED -> 2;
            case OPTIONAL -> 1;
        };
    }

    private static MappedRequirement strongerMapping(MappedRequirement first, MappedRequirement second) {
        return rank(second.importance()) > rank(first.importance()) ? second : first;
    }

    private static double frequency(int count, int processedCount) {
        return (double) count / processedCount;
    }

    private static final class MutableCounts {
        private final CanonicalRequirement requirement;
        private int required;
        private int preferred;
        private int optional;

        private MutableCounts(CanonicalRequirement requirement) {
            this.requirement = requirement;
        }

        private void increment(RequirementImportance importance) {
            switch (importance) {
                case REQUIRED -> required++;
                case PREFERRED -> preferred++;
                case OPTIONAL -> optional++;
            }
        }

        private int total() {
            return required + preferred + optional;
        }

        private RequirementStatistics toStatistics(int processedCount) {
            return new RequirementStatistics(requirement.id(), requirement.label(), requirement.type(), total(),
                    required, preferred, optional, frequency(total(), processedCount),
                    frequency(required, processedCount), frequency(preferred, processedCount),
                    frequency(optional, processedCount));
        }
    }
}
