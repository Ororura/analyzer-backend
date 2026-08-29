package com.ororura.analyzer.ats.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.ororura.analyzer.ats.domain.AnalysisInsights;
import com.ororura.analyzer.ats.domain.AnalysisInsights.Recommendation;
import com.ororura.analyzer.ats.domain.AtsScores.ScoreAssessment;
import com.ororura.analyzer.ats.domain.CandidateAssessment;
import com.ororura.analyzer.ats.domain.CandidateAssessment.CandidateExperience;
import com.ororura.analyzer.ats.domain.CandidateAssessment.DetectedLevel;
import com.ororura.analyzer.ats.domain.CandidateAssessment.ExperienceAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.FilterAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.FilterStatus;
import com.ororura.analyzer.ats.domain.MatchingAssessment.ResumeFilterAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyStatus;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis.NormalizedTechnologyAssessment;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmExperience;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmExperienceAssessment;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmFilterAssessment;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmRecommendation;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmStructuredFilters;
import org.springframework.stereotype.Component;

@Component
public class AtsAnalysisNormalizer {

    private static final String DIAGNOSTIC_REASON = "unknown_salary_assumption";
    private static final Pattern SALARY_ASSUMPTION = Pattern.compile(
            "(?:зарплат[^.!?\\n]{0,60}\\d|\\d[\\d\\s]*\\s*(?:₽|руб(?:л(?:ей|я)?)?))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public NormalizationResult normalize(LlmAtsAnalysis analysis) {
        List<NormalizationDiagnostic> diagnostics = new ArrayList<>();
        boolean sanitizeSalaryAssumptions = analysis.structuredFilters().salary().status().equals("unknown");

        List<String> hhSearchEvidence = sanitizeSalaryAssumptions
                ? removeAssumptions(analysis.scoreEvidence().hhSearchMatch(),
                        "atsAnalysis.scoreEvidence.hhSearchMatch", diagnostics)
                : analysis.scoreEvidence().hhSearchMatch();
        List<String> readabilityEvidence = sanitizeSalaryAssumptions
                ? removeAssumptions(analysis.scoreEvidence().recruiterReadability(),
                        "atsAnalysis.scoreEvidence.recruiterReadability", diagnostics)
                : analysis.scoreEvidence().recruiterReadability();
        List<String> levelFitEvidence = sanitizeSalaryAssumptions
                ? removeAssumptions(analysis.scoreEvidence().targetLevelFit(),
                        "atsAnalysis.scoreEvidence.targetLevelFit", diagnostics)
                : analysis.scoreEvidence().targetLevelFit();

        List<String> strengths = sanitizeSalaryAssumptions
                ? removeAssumptions(analysis.strengths(), "atsAnalysis.strengths", diagnostics)
                : analysis.strengths();
        List<String> weaknesses = sanitizeSalaryAssumptions
                ? removeAssumptions(analysis.weaknesses(), "atsAnalysis.weaknesses", diagnostics)
                : analysis.weaknesses();
        List<String> risks = sanitizeSalaryAssumptions
                ? removeAssumptions(analysis.recruiterRisks(), "atsAnalysis.recruiterRisks", diagnostics)
                : analysis.recruiterRisks();
        List<Recommendation> recommendations = mapRecommendations(
                analysis.recommendations(), sanitizeSalaryAssumptions, diagnostics);

        String summary = analysis.summary();
        if (sanitizeSalaryAssumptions && hasSalaryAssumption(summary)) {
            diagnostics.add(new NormalizationDiagnostic("atsAnalysis.summary", DIAGNOSTIC_REASON));
            summary = removeSummaryAssumptions(summary);
        }

        NormalizedAtsAnalysis result = new NormalizedAtsAnalysis(
                new CandidateAssessment(detectedLevel(analysis.detectedLevel()), experience(analysis.experience())),
                filters(analysis.structuredFilters()),
                analysis.technologies().stream()
                        .map(item -> new NormalizedTechnologyAssessment(
                                item.technology(), technologyStatus(item.status()), item.evidence()))
                        .toList(),
                new ScoreAssessment(analysis.hhSearchMatch(), hhSearchEvidence),
                new ScoreAssessment(analysis.recruiterReadability(), readabilityEvidence),
                new ScoreAssessment(analysis.targetLevelFit(), levelFitEvidence),
                new AnalysisInsights(strengths, weaknesses, risks, recommendations, summary));
        return new NormalizationResult(result, diagnostics);
    }

    private List<Recommendation> mapRecommendations(
            List<LlmRecommendation> values,
            boolean sanitizeSalaryAssumptions,
            List<NormalizationDiagnostic> diagnostics) {
        List<Recommendation> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            LlmRecommendation item = values.get(index);
            if (sanitizeSalaryAssumptions && (hasSalaryAssumption(item.problem())
                    || hasSalaryAssumption(item.recommendation()) || hasSalaryAssumption(item.evidence()))) {
                diagnostics.add(new NormalizationDiagnostic(
                        "atsAnalysis.recommendations." + index, DIAGNOSTIC_REASON));
            } else {
                result.add(new Recommendation(item.priority(), item.section(), item.problem(), item.recommendation(),
                        item.example(), item.evidence()));
            }
        }
        return List.copyOf(result);
    }

    private List<String> removeAssumptions(
            List<String> values,
            String path,
            List<NormalizationDiagnostic> diagnostics) {
        List<String> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            if (hasSalaryAssumption(values.get(index))) {
                diagnostics.add(new NormalizationDiagnostic(path + "." + index, DIAGNOSTIC_REASON));
            } else {
                result.add(values.get(index));
            }
        }
        return List.copyOf(result);
    }

    private String removeSummaryAssumptions(String summary) {
        String normalized = Pattern.compile("(?<=[.!?])\\s+").splitAsStream(summary)
                .filter(sentence -> !hasSalaryAssumption(sentence))
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        return normalized.isEmpty()
                ? "Недостаточно непротиворечивых данных для итогового резюме."
                : normalized;
    }

    private boolean hasSalaryAssumption(String value) {
        return value != null && SALARY_ASSUMPTION.matcher(value).find();
    }

    private CandidateExperience experience(LlmExperience value) {
        return new CandidateExperience(
                experienceAssessment(value.totalExperience()),
                experienceAssessment(value.relevantJavaExperience()),
                experienceAssessment(value.backendExperience()),
                experienceAssessment(value.commercialExperience()),
                experienceAssessment(value.projectExperience()));
    }

    private ExperienceAssessment experienceAssessment(LlmExperienceAssessment value) {
        return new ExperienceAssessment(value.value(), value.evidence());
    }

    private ResumeFilterAssessment filters(LlmStructuredFilters value) {
        return new ResumeFilterAssessment(
                filter(value.experience()), filter(value.education()), filter(value.location()),
                filter(value.relocation()), filter(value.salary()), filter(value.languages()),
                filter(value.employmentType()), filter(value.workFormat()));
    }

    private FilterAssessment filter(LlmFilterAssessment value) {
        return new FilterAssessment(filterStatus(value.status()), value.evidence());
    }

    private DetectedLevel detectedLevel(String value) {
        return DetectedLevel.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private FilterStatus filterStatus(String value) {
        return FilterStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private TechnologyStatus technologyStatus(String value) {
        return TechnologyStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
