package com.ororura.analyzer.market.requirement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.vacancy.domain.Vacancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VacancyRequirementPipeline {

    private static final Logger log = LoggerFactory.getLogger(VacancyRequirementPipeline.class);
    private final VacancyRequirementExtractor semanticExtractor;
    private final KnownRequirementExtractor knownExtractor;
    private final VacancyRequirementMerger merger;
    private final VacancyRequirementAggregator aggregator;
    private final MarketRequirementProperties properties;

    public VacancyRequirementPipeline(VacancyRequirementExtractor semanticExtractor,
            KnownRequirementExtractor knownExtractor, VacancyRequirementMerger merger,
            VacancyRequirementAggregator aggregator, MarketRequirementProperties properties) {
        this.semanticExtractor = semanticExtractor;
        this.knownExtractor = knownExtractor;
        this.merger = merger;
        this.aggregator = aggregator;
        this.properties = properties;
    }

    public MarketRequirementStatistics analyze(ResumeAnalysisProfile profile, List<Vacancy> vacancies) {
        if (profile == null || vacancies == null) {
            throw new IllegalArgumentException("Profile and vacancy pool must be defined");
        }
        PreparedPool pool = prepare(vacancies);
        List<CanonicalVacancyRequirements> processed = new ArrayList<>();
        for (List<VacancyRequirementInput> batch : batches(pool.inputs(), properties.batchSize())) {
            processResilient(profile, batch, processed);
        }
        return aggregator.aggregate(profile, pool.fetchedCount(), processed);
    }

    public List<ResolvedVacancyRequirements> resolve(ResumeAnalysisProfile profile, List<Vacancy> vacancies) {
        if (profile == null || vacancies == null) throw new IllegalArgumentException("Profile and vacancies are required");
        PreparedPool pool = prepare(vacancies);
        List<CanonicalVacancyRequirements> processed = new ArrayList<>();
        for (List<VacancyRequirementInput> batch : batches(pool.inputs(), properties.batchSize())) {
            processResilient(profile, batch, processed);
        }
        return processed.stream().map(vacancy -> new ResolvedVacancyRequirements(vacancy.vacancyId(),
                vacancy.requirements().stream().map(value -> new ResolvedVacancyRequirements.Requirement(
                        value.id(), value.label(), value.type(), value.importance())).toList())).toList();
    }

    private void processResilient(ResumeAnalysisProfile profile, List<VacancyRequirementInput> batch,
            List<CanonicalVacancyRequirements> target) {
        try {
            merge(profile, batch, extractWithRetries(batch), target);
        } catch (RuntimeException exception) {
            if (batch.size() == 1) {
                log.warn("Vacancy requirement extraction skipped vacancyId={} after retries",
                        batch.getFirst().vacancyId());
                return;
            }
            log.warn("Vacancy requirement batch extraction failed batchSize={}; isolating vacancies", batch.size());
            for (VacancyRequirementInput vacancy : batch) {
                processResilient(profile, List.of(vacancy), target);
            }
        }
    }

    private List<ExtractedVacancyRequirements> extractWithRetries(List<VacancyRequirementInput> batch) {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            try {
                return semanticExtractor.extract(batch);
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn("Vacancy requirement extraction attempt failed attempt={} maxRetries={} batchSize={} ids={}",
                        attempt + 1, properties.maxRetries(), batch.size(),
                        batch.stream().map(VacancyRequirementInput::vacancyId).toList());
            }
        }
        throw lastFailure;
    }

    private void merge(ResumeAnalysisProfile profile, List<VacancyRequirementInput> batch,
            List<ExtractedVacancyRequirements> semantic, List<CanonicalVacancyRequirements> target) {
        Map<String, VacancyRequirementInput> inputs = batch.stream().collect(java.util.stream.Collectors.toMap(
                VacancyRequirementInput::vacancyId, value -> value, (first, second) -> first, LinkedHashMap::new));
        for (ExtractedVacancyRequirements extracted : semantic) {
            VacancyRequirementInput input = inputs.get(extracted.vacancyId());
            List<ExtractedRequirement> known = knownExtractor.extract(profile, input);
            target.add(merger.merge(profile, extracted.vacancyId(), extracted.requirements(), known));
        }
    }

    private static PreparedPool prepare(List<Vacancy> vacancies) {
        Map<String, VacancyRequirementInput> unique = new LinkedHashMap<>();
        int invalid = 0;
        for (Vacancy vacancy : vacancies) {
            if (vacancy == null) {
                invalid++;
                continue;
            }
            String id = firstNonBlank(vacancy.id(), vacancy.hhId());
            if (id == null || vacancy.title() == null || vacancy.title().isBlank()) {
                invalid++;
                continue;
            }
            unique.putIfAbsent(id, new VacancyRequirementInput(id, vacancy.title(), vacancy.description()));
        }
        return new PreparedPool(List.copyOf(unique.values()), unique.size() + invalid);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second != null && !second.isBlank() ? second : null;
    }

    private static <T> List<List<T>> batches(List<T> values, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int start = 0; start < values.size(); start += size) {
            result.add(values.subList(start, Math.min(start + size, values.size())));
        }
        return result;
    }

    private record PreparedPool(List<VacancyRequirementInput> inputs, int fetchedCount) {
    }
}
