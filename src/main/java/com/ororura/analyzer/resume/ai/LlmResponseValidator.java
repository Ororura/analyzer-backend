package com.ororura.analyzer.resume.ai;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.ororura.analyzer.resume.domain.ExperienceModels.EmploymentPeriod;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.springframework.stereotype.Component;

@Component
public class LlmResponseValidator {

    private static final int EARLIEST_YEAR = 1950;
    private final Clock clock;

    public LlmResponseValidator(Clock clock) {
        this.clock = clock;
    }

    public List<EmploymentPeriod> validateAndConvert(LlmResumeAnalysisResponse response) {
        YearMonth currentMonth = YearMonth.now(clock);
        List<EmploymentPeriod> periods = new ArrayList<>();
        for (LlmResumeAnalysisResponse.EmploymentPeriod value : response.employmentPeriods()) {
            validateText(value.company());
            validateText(value.position());
            validateDateParts(value.startYear(), value.startMonth(), currentMonth);
            validateDateParts(value.endYear(), value.endMonth(), currentMonth);
            if (value.current()) {
                if (value.endYear() != null || value.endMonth() != null) {
                    throw invalid();
                }
            }
            EmploymentPeriod period = conservativePeriod(value, currentMonth);
            if (period != null) {
                periods.add(period);
            }
        }
        validateScores(response);
        validateStrings(response.skills().confirmed());
        validateStrings(response.skills().weakEvidence());
        validateStrings(response.skills().missing());
        validateStrings(response.strengths());
        validateStrings(response.weaknesses());
        validateStrings(response.atsIssues());
        validateStrings(response.recommendations());
        validateStrings(response.warnings());
        return List.copyOf(periods);
    }

    private static EmploymentPeriod conservativePeriod(
            LlmResumeAnalysisResponse.EmploymentPeriod value,
            YearMonth currentMonth) {
        if (value.startYear() == null || (!value.current() && value.endYear() == null)) {
            return null;
        }
        if (!value.current() && value.endYear() < value.startYear()) {
            throw invalid();
        }
        YearMonth start = YearMonth.of(value.startYear(), value.startMonth() == null ? 12 : value.startMonth());
        YearMonth end = value.current()
                ? currentMonth
                : YearMonth.of(value.endYear(), value.endMonth() == null ? 1 : value.endMonth());
        if (start.isAfter(currentMonth) || end.isBefore(start)) {
            if (value.startMonth() != null && (value.current() || value.endMonth() != null)) {
                throw invalid();
            }
            return null;
        }
        return new EmploymentPeriod(start, end);
    }

    private static void validateDateParts(Integer year, Integer month, YearMonth currentMonth) {
        if (year != null && (year < EARLIEST_YEAR || year > currentMonth.getYear())) {
            throw invalid();
        }
        if (month != null && (month < 1 || month > 12)) {
            throw invalid();
        }
        if (year != null && month != null && YearMonth.of(year, month).isAfter(currentMonth)) {
            throw invalid();
        }
    }

    private static void validateScores(LlmResumeAnalysisResponse response) {
        var technical = response.technicalAssessment();
        validateScore(technical.javaDepth(), technical.springDepth(), technical.backendDepth(),
                technical.sqlPostgresqlDepth(), technical.hibernateJpaDepth(), technical.infrastructureDepth(),
                technical.messagingCacheDepth(), technical.testingDepth());
        var experience = response.experienceAssessment();
        validateScore(experience.commercialRelevance(), experience.experienceDescriptionQuality(),
                experience.responsibilityLevel());
        validateScore(response.resumeAssessment().resumeQuality(), response.resumeAssessment().atsReadability());
    }

    private static void validateScore(LlmResumeAnalysisResponse.SemanticScore... scores) {
        for (LlmResumeAnalysisResponse.SemanticScore score : scores) {
            validateStrings(score.evidence());
        }
    }

    private static void validateStrings(List<String> values) {
        values.forEach(LlmResponseValidator::validateText);
    }

    private static void validateText(String value) {
        if (value == null || value.isBlank()) throw invalid();
    }

    private static ResumeAnalysisException invalid() {
        return new ResumeAnalysisException(ResumeErrorCode.AI_INVALID_RESPONSE,
                "AI response contains invalid semantic data");
    }
}
