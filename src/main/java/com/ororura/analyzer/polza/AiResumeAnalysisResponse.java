package com.ororura.analyzer.polza;

import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static com.ororura.analyzer.ats.AtsModels.*;

public record AiResumeAnalysisResponse(BasicAnalysis basicAnalysis, AiAtsAnalysis atsAnalysis) {

    public AiResumeAnalysisResponse {
        if (basicAnalysis == null) throw new IllegalArgumentException("basicAnalysis is required");
        if (atsAnalysis == null) throw new IllegalArgumentException("atsAnalysis is required");
    }

    public record BasicAnalysis(
            String level,
            BasicScores scores,
            int candidateStrength,
            int resumeQuality,
            Integer vacancyFit,
            String hrScreeningChance,
            String technicalInterviewChance,
            List<String> strengths,
            List<String> problems,
            List<AiExperienceAnalysis> experienceAnalysis,
            List<String> recommendedVacancies,
            List<String> beforeMassApplications,
            List<String> studyPriority,
            List<String> notNeededNow) {
        public BasicAnalysis {
            requireOneOf(level, Set.of("junior", "junior_plus", "junior_middle", "middle", "insufficient_data"), "level");
            requireTenPoint(candidateStrength, "candidateStrength");
            requireTenPoint(resumeQuality, "resumeQuality");
            if (vacancyFit != null) requireTenPoint(vacancyFit, "vacancyFit");
            requireOneOf(hrScreeningChance, Set.of("low", "medium", "high"), "hrScreeningChance");
            requireOneOf(technicalInterviewChance, Set.of("low", "medium", "high"), "technicalInterviewChance");
            strengths = copyOrEmpty(strengths);
            problems = copyOrEmpty(problems);
            experienceAnalysis = copyOrEmpty(experienceAnalysis);
            recommendedVacancies = copyOrEmpty(recommendedVacancies);
            beforeMassApplications = copyOrEmpty(beforeMassApplications);
            studyPriority = copyOrEmpty(studyPriority);
            notNeededNow = copyOrEmpty(notNeededNow);
        }
    }

    public record BasicScores(
            int java,
            int spring,
            int backend,
            int sqlPostgresql,
            int hibernateJpa,
            int infrastructure,
            int messagingCache,
            int testing,
            int commercialExperience,
            int projects,
            int experienceDescription,
            int levelFit,
            int ats,
            int resumeQuality,
            int education) {
        public BasicScores {
            int[] values = {java, spring, backend, sqlPostgresql, hibernateJpa, infrastructure, messagingCache,
                    testing, commercialExperience, projects, experienceDescription, levelFit, ats, resumeQuality, education};
            for (int value : values) requireTenPoint(value, "basic score");
        }
    }

    public record AiExperienceAnalysis(
            String company,
            String role,
            String startDate,
            String endDate,
            List<String> confirmedTechnologies,
            String assessment,
            List<String> issues,
            List<Rewrite> rewrites) {
        public AiExperienceAnalysis {
            validateDate(startDate, false);
            validateDate(endDate, true);
            confirmedTechnologies = copyOrEmpty(confirmedTechnologies);
            issues = copyOrEmpty(issues);
            rewrites = copyOrEmpty(rewrites);
            if (assessment == null || assessment.trim().isEmpty()) {
                throw new IllegalArgumentException("assessment is required");
            }
        }
    }

    public record Rewrite(String original, String problem, String improved) {
    }

    public record AiAtsAnalysis(
            int hhSearchMatch,
            int recruiterReadability,
            DetectedLevel detectedLevel,
            int targetLevelFit,
            AiExperience experience,
            StructuredFilters structuredFilters,
            List<RawTechnologyAssessment> technologies,
            ScoreEvidence scoreEvidence,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recruiterRisks,
            List<Recommendation> recommendations,
            String summary) {
        public AiAtsAnalysis {
            requireScore(hhSearchMatch, "hhSearchMatch");
            requireScore(recruiterReadability, "recruiterReadability");
            requireScore(targetLevelFit, "targetLevelFit");
            technologies = List.copyOf(technologies);
            strengths = List.copyOf(strengths);
            weaknesses = List.copyOf(weaknesses);
            recruiterRisks = List.copyOf(recruiterRisks);
            recommendations = List.copyOf(recommendations);
            Set<String> technologyNames = new HashSet<>();
            for (RawTechnologyAssessment technology : technologies) {
                if (!technologyNames.add(technology.technology().toLowerCase(Locale.forLanguageTag("ru-RU")))) {
                    throw new IllegalArgumentException("Технологии не должны дублироваться");
                }
            }
            if (summary == null || summary.trim().isEmpty()) {
                throw new IllegalArgumentException("summary is required");
            }
            summary = summary.trim();
        }
    }

    public record AiExperience(
            AiExperienceAssessment totalExperience,
            AiExperienceAssessment relevantJavaExperience,
            AiExperienceAssessment backendExperience,
            AiExperienceAssessment commercialExperience,
            AiExperienceAssessment projectExperience) {
    }

    public record AiExperienceAssessment(List<ExperiencePeriod> periods, String evidence) {
        public AiExperienceAssessment {
            periods = List.copyOf(periods);
            evidence = evidence == null || evidence.trim().isEmpty() ? null : evidence.trim();
            if (!periods.isEmpty() && evidence == null) {
                throw new IllegalArgumentException("Для извлечённых периодов требуется evidence");
            }
        }
    }

    public record ExperiencePeriod(String startDate, String endDate) {
        public ExperiencePeriod {
            validateDate(startDate, false);
            validateDate(endDate, true);
        }
    }

    private static final Pattern EXPERIENCE_DATE = Pattern.compile(
            "^\\d{4}(?:-(?:0[1-9]|1[0-2])(?:-(?:0[1-9]|[12]\\d|3[01]))?)?$");

    private static void requireScore(int score, String name) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(name + " must be in range 0..100");
        }
    }

    private static void requireTenPoint(int score, String name) {
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException(name + " must be in range 0..10");
        }
    }

    private static void requireOneOf(String value, Set<String> allowed, String name) {
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(name + " has an unsupported value");
        }
    }

    private static void validateDate(String value, boolean allowPresent) {
        if (value == null) return;
        if (allowPresent && value.equals("present")) return;
        if (!EXPERIENCE_DATE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid experience date");
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
