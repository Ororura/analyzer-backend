package com.ororura.analyzer.ats;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import static com.ororura.analyzer.ats.AtsModels.*;

@Component
public class AtsNormalizer {

    private static final String DIAGNOSTIC_REASON = "unknown_salary_assumption";
    private static final Pattern SALARY_ASSUMPTION = Pattern.compile(
            "(?:зарплат[^.!?\\n]{0,60}\\d|\\d[\\d\\s]*\\s*(?:₽|руб(?:л(?:ей|я)?)?))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public NormalizationResult normalizeAiResumeAnalysis(RawAtsAnalysis analysis) {
        if (analysis.structuredFilters().salary().status() != FilterStatus.UNKNOWN) {
            return new NormalizationResult(analysis, List.of());
        }

        List<NormalizationDiagnostic> diagnostics = new ArrayList<>();
        ScoreEvidence evidence = new ScoreEvidence(
                removeAssumptions(analysis.scoreEvidence().hhSearchMatch(),
                        "atsAnalysis.scoreEvidence.hhSearchMatch", diagnostics),
                removeAssumptions(analysis.scoreEvidence().recruiterReadability(),
                        "atsAnalysis.scoreEvidence.recruiterReadability", diagnostics),
                removeAssumptions(analysis.scoreEvidence().targetLevelFit(),
                        "atsAnalysis.scoreEvidence.targetLevelFit", diagnostics));
        List<String> strengths = removeAssumptions(analysis.strengths(), "atsAnalysis.strengths", diagnostics);
        List<String> weaknesses = removeAssumptions(analysis.weaknesses(), "atsAnalysis.weaknesses", diagnostics);
        List<String> risks = removeAssumptions(analysis.recruiterRisks(), "atsAnalysis.recruiterRisks", diagnostics);

        List<Recommendation> recommendations = new ArrayList<>();
        for (int index = 0; index < analysis.recommendations().size(); index++) {
            Recommendation item = analysis.recommendations().get(index);
            if (hasSalaryAssumption(item.problem()) || hasSalaryAssumption(item.recommendation())
                    || hasSalaryAssumption(item.evidence())) {
                diagnostics.add(new NormalizationDiagnostic("atsAnalysis.recommendations." + index, DIAGNOSTIC_REASON));
            } else {
                recommendations.add(item);
            }
        }

        String summary = analysis.summary();
        if (hasSalaryAssumption(summary)) {
            diagnostics.add(new NormalizationDiagnostic("atsAnalysis.summary", DIAGNOSTIC_REASON));
            summary = removeSummaryAssumptions(summary);
        }

        RawAtsAnalysis result = new RawAtsAnalysis(
                analysis.hhSearchMatch(), analysis.recruiterReadability(), analysis.detectedLevel(),
                analysis.targetLevelFit(), analysis.experience(), analysis.structuredFilters(), analysis.technologies(),
                evidence, strengths, weaknesses, risks, recommendations, summary);
        return new NormalizationResult(result, List.copyOf(diagnostics));
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
}
