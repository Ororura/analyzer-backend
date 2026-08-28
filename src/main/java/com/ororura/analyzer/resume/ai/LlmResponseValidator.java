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
            YearMonth start = yearMonth(value.startYear(), value.startMonth(), currentMonth);
            YearMonth end;
            if (value.current()) {
                if (value.endYear() != null || value.endMonth() != null) throw invalid();
                end = currentMonth;
            } else {
                if (value.endYear() == null || value.endMonth() == null) throw invalid();
                end = yearMonth(value.endYear(), value.endMonth(), currentMonth);
            }
            if (end.isBefore(start)) throw invalid();
            periods.add(new EmploymentPeriod(start, end));
        }
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

    private static YearMonth yearMonth(int year, int month, YearMonth currentMonth) {
        if (year < EARLIEST_YEAR || year > currentMonth.getYear() || month < 1 || month > 12) throw invalid();
        YearMonth value = YearMonth.of(year, month);
        if (value.isAfter(currentMonth)) throw invalid();
        return value;
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
