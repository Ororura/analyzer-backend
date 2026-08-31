package com.ororura.analyzer.resume.ai;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.ororura.analyzer.resume.domain.ExperienceModels.EmploymentPeriod;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LlmResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(LlmResponseValidator.class);
    private static final int EARLIEST_YEAR = 1950;
    private final Clock clock;

    public LlmResponseValidator(Clock clock) {
        this.clock = clock;
    }

    public List<EmploymentPeriod> validateAndConvert(LlmResumeAnalysisResponse response,
            MarketAnalysisProfile profile) {
        YearMonth currentMonth = YearMonth.now(clock);
        List<EmploymentPeriod> periods = new ArrayList<>();
        for (LlmResumeAnalysisResponse.EmploymentPeriod value : response.employmentPeriods()) {
            validateText(value.company());
            validateText(value.position());
            validateDateParts(value.startYear(), value.startMonth(), currentMonth);
            validateDateParts(value.endYear(), value.endMonth(), currentMonth);
            if (value.current()) {
                if (value.endYear() != null || value.endMonth() != null) {
                    throw invalid("current employment period contains an end date");
                }
            }
            EmploymentPeriod period = conservativePeriod(value, currentMonth);
            if (period != null) {
                periods.add(period);
            }
        }
        validateScores(response, profile);
        validateSkills(response, profile);
        validateStructuredFacts(response, profile);
        validateStrings(response.strengths());
        validateStrings(response.weaknesses());
        validateStrings(response.atsIssues());
        validateStrings(response.recommendations());
        validateStrings(response.warnings());
        return List.copyOf(periods);
    }

    private static void validateStructuredFacts(LlmResumeAnalysisResponse response, MarketAnalysisProfile profile) {
        java.util.Set<String> allowed = profile.requirements().stream().map(MarketRequirement::label)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (var skill : response.skillEvidence()) {
            validateText(skill.skill());
            if (!allowed.contains(skill.skill())) throw invalid("skill evidence contains an unknown skill");
            if (!seen.add(skill.skill().toLowerCase(java.util.Locale.ROOT))) {
                throw invalid("skill evidence contains a duplicate skill");
            }
            for (var evidence : skill.evidence()) validateText(evidence.text());
        }
        java.util.Set<String> fields = new java.util.HashSet<>();
        for (var field : response.atsFields()) {
            validateText(field.field()); validateStrings(field.issues());
            if (!fields.add(field.field().toLowerCase(java.util.Locale.ROOT))) {
                throw invalid("ATS fields contain a duplicate field");
            }
        }
        for (var claim : response.claims()) {
            validateText(claim.claim()); validateText(claim.explanation());
        }
        int questions = 0;
        for (var topic : response.interviewTopics()) {
            validateText(topic.topic()); validateText(topic.sourceClaim()); validateStrings(topic.questions());
            if (topic.questions().size() > 3) throw invalid("an interview topic contains more than three questions");
            questions += topic.questions().size();
        }
        if (questions > 15) throw invalid("interview topics contain more than fifteen questions in total");
        if (response.interviewTopics().size() > 15) {
            throw invalid("response contains more than fifteen interview topics");
        }
    }

    private static EmploymentPeriod conservativePeriod(
            LlmResumeAnalysisResponse.EmploymentPeriod value,
            YearMonth currentMonth) {
        if (value.startYear() == null || (!value.current() && value.endYear() == null)) {
            return null;
        }
        if (!value.current() && value.endYear() < value.startYear()) {
            throw invalid("employment period ends before its start year");
        }
        YearMonth start = YearMonth.of(value.startYear(), value.startMonth() == null ? 12 : value.startMonth());
        YearMonth end = value.current()
                ? currentMonth
                : YearMonth.of(value.endYear(), value.endMonth() == null ? 1 : value.endMonth());
        if (start.isAfter(currentMonth) || end.isBefore(start)) {
            if (value.startMonth() != null && (value.current() || value.endMonth() != null)) {
                throw invalid("employment period ends before it starts");
            }
            return null;
        }
        return new EmploymentPeriod(start, end);
    }

    private static void validateDateParts(Integer year, Integer month, YearMonth currentMonth) {
        if (year != null && (year < EARLIEST_YEAR || year > currentMonth.getYear())) {
            throw invalid("employment date is outside the supported year range");
        }
        if (month != null && (month < 1 || month > 12)) {
            throw invalid("employment month is outside the range 1..12");
        }
        if (year != null && month != null && YearMonth.of(year, month).isAfter(currentMonth)) {
            throw invalid("employment date is in the future");
        }
    }

    private static void validateScores(LlmResumeAnalysisResponse response, MarketAnalysisProfile profile) {
        java.util.Set<String> expected = profile.criteria().stream()
                .map(AnalysisCriterion::id).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> actual = new java.util.HashSet<>();
        for (CriterionAssessment assessment : response.assessments()) {
            if (!actual.add(assessment.criterionId())) throw invalid("criterion assessments contain a duplicate id");
            validateStrings(assessment.evidence());
        }
        if (!actual.equals(expected)) throw invalid("criterion assessment ids do not match the requested profile");
        var experience = response.experienceAssessment();
        validateScore(experience.commercialRelevance(), experience.experienceDescriptionQuality(),
                experience.responsibilityLevel());
        validateScore(response.resumeAssessment().resumeQuality(), response.resumeAssessment().atsReadability());
    }

    private static void validateSkills(LlmResumeAnalysisResponse response, MarketAnalysisProfile profile) {
        java.util.Set<String> allowed = profile.requirements().stream()
                .map(MarketRequirement::label).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> allowedMissing = profile.requirements().stream()
                .filter(requirement -> requirement.frequency() > 0).map(MarketRequirement::label)
                .collect(java.util.stream.Collectors.toSet());
        validateSkillList(response.skills().confirmed(), allowed);
        validateSkillList(response.skills().weakEvidence(), allowed);
        validateSkillList(response.skills().missing(), allowedMissing);
    }

    private static void validateSkillList(List<String> values, java.util.Set<String> allowed) {
        validateStrings(values);
        if (!allowed.containsAll(values) || values.size() != new java.util.HashSet<>(values).size()) {
            throw invalid("skill list contains an unknown or duplicate value");
        }
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
        if (value == null || value.isBlank()) throw invalid("response contains a blank text value");
    }

    private static ResumeAnalysisException invalid(String reason) {
        log.warn("LLM semantic validation failed reason={}", reason);
        return new ResumeAnalysisException(ResumeErrorCode.AI_INVALID_RESPONSE,
                "AI response contains invalid semantic data", new IllegalArgumentException(reason));
    }
}
