package com.ororura.analyzer.ats;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.ororura.analyzer.ats.AtsModels.*;
import static org.assertj.core.api.Assertions.assertThat;

class AtsNormalizerTests {

    private final AtsNormalizer normalizer = new AtsNormalizer();

    @ParameterizedTest
    @CsvSource({
            "'Ожидания по зарплате 120 000 руб.',true",
            "'Ожидания 180000 ₽',true",
            "'Опыт Java подтверждён',false"
    })
    void removesSalaryAssumptionsOnlyWhenSalaryIsUnknown(String statement, boolean removed) {
        RawAtsAnalysis raw = withTargetEvidence(AtsFixture.raw(), statement);

        NormalizationResult normalized = normalizer.normalizeAiResumeAnalysis(raw);

        assertThat(normalized.result().scoreEvidence().targetLevelFit().contains(statement)).isEqualTo(!removed);
        assertThat(normalized.diagnostics().isEmpty()).isEqualTo(!removed);
    }

    @Test
    void removesSalaryAssumptionsFromAllNormalizedFields() {
        RawAtsAnalysis raw = AtsFixture.raw();
        RawAtsAnalysis withAssumptions = new RawAtsAnalysis(
                raw.hhSearchMatch(), raw.recruiterReadability(), raw.detectedLevel(), raw.targetLevelFit(),
                raw.experience(), raw.structuredFilters(), raw.technologies(),
                new ScoreEvidence(List.of("Java", "Зарплата 120 000 руб."), List.of("Readable"), List.of("120000 ₽")),
                List.of("Сильный Java"),
                List.of("Ожидания по зарплате 120000 руб."),
                List.of("Риск не связан с зарплатой"),
                List.of(new Recommendation("high", "Условия", "Зарплата 120 000 руб.",
                        "Указать ожидания", null, null)),
                "Подходит по Java. Зарплата 120 000 руб. требует уточнения.");

        NormalizationResult result = normalizer.normalizeAiResumeAnalysis(withAssumptions);

        assertThat(result.result().scoreEvidence().hhSearchMatch()).containsExactly("Java");
        assertThat(result.result().scoreEvidence().targetLevelFit()).isEmpty();
        assertThat(result.result().weaknesses()).isEmpty();
        assertThat(result.result().recommendations()).isEmpty();
        assertThat(result.result().summary()).isEqualTo("Подходит по Java. требует уточнения.");
        assertThat(result.diagnostics()).extracting(NormalizationDiagnostic::reason)
                .containsOnly("unknown_salary_assumption");
    }

    @Test
    void doesNotNormalizeSalaryClaimsWhenSalaryFilterIsKnown() {
        RawAtsAnalysis raw = AtsFixture.raw();
        StructuredFilters filters = raw.structuredFilters();
        RawAtsAnalysis knownSalary = withFilters(raw, new StructuredFilters(
                filters.experience(), filters.education(), filters.location(), filters.relocation(),
                new FilterAssessment(FilterStatus.MATCH, "120 000 руб."), filters.languages(),
                filters.employmentType(), filters.workFormat()));

        NormalizationResult result = normalizer.normalizeAiResumeAnalysis(
                withTargetEvidence(knownSalary, "Зарплата 120 000 руб."));

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.result().scoreEvidence().targetLevelFit()).containsExactly("Зарплата 120 000 руб.");
    }

    private static RawAtsAnalysis withTargetEvidence(RawAtsAnalysis raw, String value) {
        return new RawAtsAnalysis(
                raw.hhSearchMatch(), raw.recruiterReadability(), raw.detectedLevel(), raw.targetLevelFit(),
                raw.experience(), raw.structuredFilters(), raw.technologies(),
                new ScoreEvidence(raw.scoreEvidence().hhSearchMatch(), raw.scoreEvidence().recruiterReadability(), List.of(value)),
                raw.strengths(), raw.weaknesses(), raw.recruiterRisks(), raw.recommendations(), raw.summary());
    }

    private static RawAtsAnalysis withFilters(RawAtsAnalysis raw, StructuredFilters filters) {
        return new RawAtsAnalysis(
                raw.hhSearchMatch(), raw.recruiterReadability(), raw.detectedLevel(), raw.targetLevelFit(),
                raw.experience(), filters, raw.technologies(), raw.scoreEvidence(), raw.strengths(), raw.weaknesses(),
                raw.recruiterRisks(), raw.recommendations(), raw.summary());
    }
}
