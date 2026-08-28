package com.ororura.analyzer.ats;

import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public final class AtsModels {

    private AtsModels() {
    }

    public enum TechnologyStatus implements JsonEnum {
        CONFIRMED_EXPERIENCE("confirmed_experience"),
        SEMANTIC_EXPERIENCE("semantic_experience"),
        EXPLICIT_OTHER("explicit_other"),
        SKILLS_ONLY("skills_only"),
        MISSING("missing"),
        IRRELEVANT("irrelevant");

        private final String value;

        TechnologyStatus(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static TechnologyStatus fromValue(String value) {
            return JsonEnum.fromValue(TechnologyStatus.values(), value);
        }
    }

    public enum FilterStatus implements JsonEnum {
        MATCH("match"), PARTIAL("partial"), MISMATCH("mismatch"), UNKNOWN("unknown");

        private final String value;

        FilterStatus(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static FilterStatus fromValue(String value) {
            return JsonEnum.fromValue(FilterStatus.values(), value);
        }
    }

    public enum DetectedLevel implements JsonEnum {
        INTERN("intern"), JUNIOR("junior"), JUNIOR_PLUS("junior_plus"), MIDDLE("middle"),
        MIDDLE_PLUS("middle_plus"), SENIOR("senior");

        private final String value;

        DetectedLevel(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static DetectedLevel fromValue(String value) {
            return JsonEnum.fromValue(DetectedLevel.values(), value);
        }
    }

    public enum TechnologyTier implements JsonEnum {
        CORE("core"), COMMON("common"), BONUS("bonus");

        private final String value;

        TechnologyTier(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static TechnologyTier fromValue(String value) {
            return JsonEnum.fromValue(TechnologyTier.values(), value);
        }
    }

    public enum ScreeningChance implements JsonEnum {
        LOW("low"), BELOW_AVERAGE("below_average"), MEDIUM("medium"), HIGH("high"), VERY_HIGH("very_high");

        private final String value;

        ScreeningChance(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static ScreeningChance fromValue(String value) {
            return JsonEnum.fromValue(ScreeningChance.values(), value);
        }
    }

    public record FilterAssessment(FilterStatus status, String evidence) {
        public FilterAssessment {
            evidence = normalizeEvidence(evidence);
            if (status == FilterStatus.UNKNOWN && evidence != null) {
                throw new IllegalArgumentException("unknown должен иметь evidence=null");
            }
            if (status != FilterStatus.UNKNOWN && evidence == null) {
                throw new IllegalArgumentException("Подтверждённый статус требует evidence");
            }
        }
    }

    public record StructuredFilters(
            FilterAssessment experience,
            FilterAssessment education,
            FilterAssessment location,
            FilterAssessment relocation,
            FilterAssessment salary,
            FilterAssessment languages,
            FilterAssessment employmentType,
            FilterAssessment workFormat) {

        public List<FilterAssessment> values() {
            return List.of(experience, education, location, relocation, salary, languages, employmentType, workFormat);
        }
    }

    public record RawTechnologyAssessment(String technology, TechnologyStatus status, String evidence) {
        public RawTechnologyAssessment {
            technology = technology == null ? "" : technology.trim();
            evidence = normalizeEvidence(evidence);
            if (technology.isEmpty()) {
                throw new IllegalArgumentException("technology must not be blank");
            }
            if (status == TechnologyStatus.MISSING && evidence != null) {
                throw new IllegalArgumentException("missing должен иметь evidence=null");
            }
            if ((status == TechnologyStatus.CONFIRMED_EXPERIENCE
                    || status == TechnologyStatus.SEMANTIC_EXPERIENCE
                    || status == TechnologyStatus.EXPLICIT_OTHER) && evidence == null) {
                throw new IllegalArgumentException("Присутствующая технология требует evidence");
            }
        }
    }

    public record TechnologyAssessment(
            String technology,
            TechnologyStatus status,
            String evidence,
            TechnologyTier tier) {
    }

    public record ExperienceAssessment(String value, String evidence) {
        public ExperienceAssessment {
            value = normalizeEvidence(value);
            evidence = normalizeEvidence(evidence);
            if (value != null && evidence == null) {
                throw new IllegalArgumentException("Для указанного опыта требуется evidence");
            }
        }
    }

    public record Experience(
            ExperienceAssessment totalExperience,
            ExperienceAssessment relevantJavaExperience,
            ExperienceAssessment backendExperience,
            ExperienceAssessment commercialExperience,
            ExperienceAssessment projectExperience) {
    }

    public record ScoreEvidence(
            List<String> hhSearchMatch,
            List<String> recruiterReadability,
            List<String> targetLevelFit) {
        public ScoreEvidence {
            hhSearchMatch = List.copyOf(hhSearchMatch);
            recruiterReadability = List.copyOf(recruiterReadability);
            targetLevelFit = List.copyOf(targetLevelFit);
        }
    }

    public record Recommendation(
            String priority,
            String section,
            String problem,
            String recommendation,
            String example,
            String evidence) {
        public Recommendation {
            evidence = normalizeEvidence(evidence);
        }
    }

    public record RawAtsAnalysis(
            int hhSearchMatch,
            int recruiterReadability,
            DetectedLevel detectedLevel,
            int targetLevelFit,
            Experience experience,
            StructuredFilters structuredFilters,
            List<RawTechnologyAssessment> technologies,
            ScoreEvidence scoreEvidence,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recruiterRisks,
            List<Recommendation> recommendations,
            String summary) {
        public RawAtsAnalysis {
            requireScore(hhSearchMatch, "hhSearchMatch");
            requireScore(recruiterReadability, "recruiterReadability");
            requireScore(targetLevelFit, "targetLevelFit");
            technologies = List.copyOf(technologies);
            Set<String> technologyNames = new HashSet<>();
            for (RawTechnologyAssessment technology : technologies) {
                if (!technologyNames.add(technology.technology().toLowerCase(Locale.forLanguageTag("ru-RU")))) {
                    throw new IllegalArgumentException("Технологии не должны дублироваться");
                }
            }
            strengths = List.copyOf(strengths);
            weaknesses = List.copyOf(weaknesses);
            recruiterRisks = List.copyOf(recruiterRisks);
            recommendations = List.copyOf(recommendations);
        }
    }

    public record Keywords(
            List<String> explicitlyPresent,
            List<String> semanticallyPresent,
            List<String> confirmedByExperience,
            List<String> skillsOnly,
            List<String> missingCore,
            List<String> missingCommon,
            List<String> optional,
            List<String> irrelevantKeywords) {
    }

    public record MarketData(String source, int sampleSize, List<String> warnings) {
    }

    public record AtsAnalysisResult(
            int hhSearchMatch,
            int recruiterReadability,
            DetectedLevel detectedLevel,
            int targetLevelFit,
            Experience experience,
            StructuredFilters structuredFilters,
            List<TechnologyAssessment> technologies,
            ScoreEvidence scoreEvidence,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recruiterRisks,
            List<Recommendation> recommendations,
            String summary,
            int atsScore,
            int hhStructuredFilters,
            int vacancyMatch,
            int keywordCoverage,
            ScreeningChance screeningChance,
            Keywords keywords,
            MarketData marketData) {
    }

    public record NormalizationDiagnostic(String path, String reason) {
    }

    public record NormalizationResult(RawAtsAnalysis result, List<NormalizationDiagnostic> diagnostics) {
    }

    private interface JsonEnum {
        String value();

        static <E extends Enum<E> & JsonEnum> E fromValue(E[] values, String value) {
            for (E candidate : values) {
                if (candidate.value().equals(value)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("Unknown enum value: " + value);
        }
    }

    private static String normalizeEvidence(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static void requireScore(int score, String name) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(name + " must be in range 0..100");
        }
    }
}
