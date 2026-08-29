package com.ororura.analyzer.ats;

import java.util.List;

import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmFilterAssessment;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmRecommendation;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmScoreEvidence;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmStructuredFilters;
import com.ororura.analyzer.ats.normalization.AtsAnalysisNormalizer;
import com.ororura.analyzer.ats.normalization.NormalizationDiagnostic;
import com.ororura.analyzer.ats.normalization.NormalizationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AtsNormalizerTests {

    private final AtsAnalysisNormalizer normalizer = new AtsAnalysisNormalizer();

    @ParameterizedTest
    @CsvSource({
            "'Ожидания по зарплате 120 000 руб.',true",
            "'Ожидания 180000 ₽',true",
            "'Опыт Java подтверждён',false"
    })
    void removesSalaryAssumptionsOnlyWhenSalaryIsUnknown(String statement, boolean removed) {
        LlmAtsAnalysis llm = withTargetEvidence(AtsFixture.llm(), statement);

        NormalizationResult normalized = normalizer.normalize(llm);

        assertThat(normalized.result().targetLevelFit().evidence().contains(statement)).isEqualTo(!removed);
        assertThat(normalized.diagnostics().isEmpty()).isEqualTo(!removed);
    }

    @Test
    void removesSalaryAssumptionsFromAllNormalizedFields() {
        LlmAtsAnalysis llm = AtsFixture.llm();
        LlmAtsAnalysis withAssumptions = new LlmAtsAnalysis(
                llm.hhSearchMatch(), llm.recruiterReadability(), llm.detectedLevel(), llm.targetLevelFit(),
                llm.experience(), llm.structuredFilters(), llm.technologies(),
                new LlmScoreEvidence(
                        List.of("Java", "Зарплата 120 000 руб."), List.of("Readable"), List.of("120000 ₽")),
                List.of("Сильный Java"),
                List.of("Ожидания по зарплате 120000 руб."),
                List.of("Риск не связан с зарплатой"),
                List.of(new LlmRecommendation(
                        "high", "Условия", "Зарплата 120 000 руб.", "Указать ожидания", null, null)),
                "Подходит по Java. Зарплата 120 000 руб. требует уточнения.");

        NormalizationResult result = normalizer.normalize(withAssumptions);

        assertThat(result.result().hhSearchMatch().evidence()).containsExactly("Java");
        assertThat(result.result().targetLevelFit().evidence()).isEmpty();
        assertThat(result.result().insights().weaknesses()).isEmpty();
        assertThat(result.result().insights().recommendations()).isEmpty();
        assertThat(result.result().insights().summary()).isEqualTo("Подходит по Java. требует уточнения.");
        assertThat(result.diagnostics()).extracting(NormalizationDiagnostic::reason)
                .containsOnly("unknown_salary_assumption");
    }

    @Test
    void doesNotNormalizeSalaryClaimsWhenSalaryFilterIsKnown() {
        LlmAtsAnalysis llm = AtsFixture.llm();
        LlmStructuredFilters filters = llm.structuredFilters();
        LlmAtsAnalysis knownSalary = withFilters(llm, new LlmStructuredFilters(
                filters.experience(), filters.education(), filters.location(), filters.relocation(),
                new LlmFilterAssessment("match", "120 000 руб."), filters.languages(),
                filters.employmentType(), filters.workFormat()));

        NormalizationResult result = normalizer.normalize(
                withTargetEvidence(knownSalary, "Зарплата 120 000 руб."));

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.result().targetLevelFit().evidence()).containsExactly("Зарплата 120 000 руб.");
    }

    @Test
    void combinesScoresWithTheirEvidence() {
        NormalizationResult result = normalizer.normalize(AtsFixture.llm());

        assertThat(result.result().hhSearchMatch().score()).isEqualTo(82);
        assertThat(result.result().hhSearchMatch().evidence())
                .containsExactly("Заголовок Java Backend Developer");
        assertThat(result.result().recruiterReadability().score()).isEqualTo(76);
        assertThat(result.result().targetLevelFit().score()).isEqualTo(79);
    }

    private static LlmAtsAnalysis withTargetEvidence(LlmAtsAnalysis value, String evidence) {
        return new LlmAtsAnalysis(
                value.hhSearchMatch(), value.recruiterReadability(), value.detectedLevel(), value.targetLevelFit(),
                value.experience(), value.structuredFilters(), value.technologies(),
                new LlmScoreEvidence(
                        value.scoreEvidence().hhSearchMatch(), value.scoreEvidence().recruiterReadability(),
                        List.of(evidence)),
                value.strengths(), value.weaknesses(), value.recruiterRisks(), value.recommendations(), value.summary());
    }

    private static LlmAtsAnalysis withFilters(LlmAtsAnalysis value, LlmStructuredFilters filters) {
        return new LlmAtsAnalysis(
                value.hhSearchMatch(), value.recruiterReadability(), value.detectedLevel(), value.targetLevelFit(),
                value.experience(), filters, value.technologies(), value.scoreEvidence(), value.strengths(),
                value.weaknesses(), value.recruiterRisks(), value.recommendations(), value.summary());
    }
}
