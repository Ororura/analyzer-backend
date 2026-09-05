package com.ororura.analyzer.resume.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ororura.analyzer.resume.ai.AiProvider;
import com.ororura.analyzer.resume.ai.AiProviderRegistry;
import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.ai.LlmResponseValidator;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import com.ororura.analyzer.resume.api.VacancyAnalysisRequest;
import com.ororura.analyzer.resume.api.VacancyAnalysisMode;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.domain.AtsScoreCalculator;
import com.ororura.analyzer.resume.domain.AtsScoreCalculator.AtsScore;
import com.ororura.analyzer.resume.domain.CandidateLevelPolicy;
import com.ororura.analyzer.resume.domain.CandidateStrengthCalculator;
import com.ororura.analyzer.resume.domain.ExperienceCalculator;
import com.ororura.analyzer.resume.domain.ExperienceModels.EmploymentPeriod;
import com.ororura.analyzer.resume.domain.ExperienceModels.ExperienceSummary;
import com.ororura.analyzer.resume.domain.InterviewChancePolicy;
import com.ororura.analyzer.resume.domain.OverallScoreCalculator;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileProvider;
import com.ororura.analyzer.resume.pdf.PdfFileValidator;
import com.ororura.analyzer.resume.pdf.PdfTextExtractor;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.vacancy.market.VacancyMarketService;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import com.ororura.analyzer.vacancy.selection.VacancySelectionException;
import com.ororura.analyzer.vacancy.provider.VacancySourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);

    private final PdfFileValidator fileValidator;
    private final PdfTextExtractor textExtractor;
    private final VacancyMarketService marketService;
    private final AiProviderRegistry aiProviderRegistry;
    private final LlmResponseValidator llmValidator;
    private final ExperienceCalculator experienceCalculator;
    private final TechnologyTaxonomy taxonomy;
    private final AtsScoreCalculator atsCalculator;
    private final OverallScoreCalculator overallCalculator;
    private final CandidateStrengthCalculator strengthCalculator;
    private final CandidateLevelPolicy levelPolicy;
    private final InterviewChancePolicy chancePolicy;
    private final ResumeAnalysisAssembler assembler;
    private final ResumeAnalysisProperties properties;
    private final Clock clock;
    private final MarketAnalysisProfileProvider marketProfileProvider;
    private final DeterministicAnalysisEngine deterministicEngine;

    @Autowired
    public ResumeAnalysisService(PdfFileValidator fileValidator, PdfTextExtractor textExtractor,
            VacancyMarketService marketService, AiProviderRegistry aiProviderRegistry, LlmResponseValidator llmValidator,
            ExperienceCalculator experienceCalculator, TechnologyTaxonomy taxonomy, AtsScoreCalculator atsCalculator,
            OverallScoreCalculator overallCalculator, CandidateStrengthCalculator strengthCalculator,
            CandidateLevelPolicy levelPolicy, InterviewChancePolicy chancePolicy, ResumeAnalysisAssembler assembler,
            ResumeAnalysisProperties properties, Clock clock, MarketAnalysisProfileProvider marketProfileProvider,
            DeterministicAnalysisEngine deterministicEngine) {
        this.fileValidator = fileValidator;
        this.textExtractor = textExtractor;
        this.marketService = marketService;
        this.aiProviderRegistry = aiProviderRegistry;
        this.llmValidator = llmValidator;
        this.experienceCalculator = experienceCalculator;
        this.taxonomy = taxonomy;
        this.atsCalculator = atsCalculator;
        this.overallCalculator = overallCalculator;
        this.strengthCalculator = strengthCalculator;
        this.levelPolicy = levelPolicy;
        this.chancePolicy = chancePolicy;
        this.assembler = assembler;
        this.properties = properties;
        this.clock = clock;
        this.marketProfileProvider = marketProfileProvider;
        this.deterministicEngine = deterministicEngine;
    }

    public ResumeAnalysisService(PdfFileValidator fileValidator, PdfTextExtractor textExtractor,
            VacancyMarketService marketService, AiProviderRegistry aiProviderRegistry, LlmResponseValidator llmValidator,
            ExperienceCalculator experienceCalculator, TechnologyTaxonomy taxonomy, AtsScoreCalculator atsCalculator,
            OverallScoreCalculator overallCalculator, CandidateStrengthCalculator strengthCalculator,
            CandidateLevelPolicy levelPolicy, InterviewChancePolicy chancePolicy, ResumeAnalysisAssembler assembler,
            ResumeAnalysisProperties properties, Clock clock, MarketAnalysisProfileProvider marketProfileProvider) {
        this(fileValidator, textExtractor, marketService, aiProviderRegistry, llmValidator, experienceCalculator,
                taxonomy, atsCalculator, overallCalculator, strengthCalculator, levelPolicy, chancePolicy, assembler,
                properties, clock, marketProfileProvider, defaultEngine(atsCalculator, overallCalculator));
    }

    public ResumeAnalysisResult analyze(MultipartFile file) {
        return analyze(file, null);
    }

    public ResumeAnalysisResult analyze(MultipartFile file, AiProviderType requestedProvider) {
        return analyze(file, requestedProvider, VacancyAnalysisRequest.autoMarket());
    }

    public ResumeAnalysisResult analyze(MultipartFile file, AiProviderType requestedProvider,
            VacancyAnalysisRequest vacancyAnalysis) {
        return analyze(file, requestedProvider, null, vacancyAnalysis);
    }

    public ResumeAnalysisResult analyze(MultipartFile file, AiProviderType requestedProvider,
            ResumeAnalysisProfile requestedProfile, VacancyAnalysisRequest vacancyAnalysis) {
        String analysisId = UUID.randomUUID().toString();
        Instant started = clock.instant();
        log.info("resume analysis started analysisId={} fileSize={}", analysisId, file == null ? 0 : file.getSize());
        try {
            return analyze(file, requestedProvider, requestedProfile, vacancyAnalysis, analysisId, started, null, (text, llm) -> {});
        } catch (VacancySelectionException exception) {
            ResumeErrorCode code = exception.getMessage().contains("maximumProcessingSize")
                    ? ResumeErrorCode.SELECTION_TOO_LARGE : ResumeErrorCode.INVALID_SELECTION;
            throw new ResumeAnalysisException(code, exception.getMessage(), exception);
        } catch (VacancySourceException exception) {
            ResumeErrorCode code = switch (exception.getCode()) {
                case "NOT_FOUND" -> ResumeErrorCode.VACANCY_NOT_FOUND;
                case "RATE_LIMITED" -> ResumeErrorCode.VACANCY_RATE_LIMITED;
                case "TIMEOUT" -> ResumeErrorCode.VACANCY_PROVIDER_TIMEOUT;
                default -> ResumeErrorCode.VACANCY_PROVIDER_FAILED;
            };
            throw new ResumeAnalysisException(code, exception.getMessage(), exception);
        } catch (ResumeAnalysisException exception) {
            log.warn("resume analysis failed analysisId={} category={}", analysisId, exception.getCode());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("resume analysis failed analysisId={} category={}", analysisId, ResumeErrorCode.ANALYSIS_FAILED);
            throw new ResumeAnalysisException(ResumeErrorCode.ANALYSIS_FAILED, "Resume analysis failed", exception);
        }
    }

    public ResumeAnalysisResult analyze(MultipartFile file,
            com.ororura.analyzer.analysis.domain.EffectiveAnalysisConfig config,
            java.util.function.BiConsumer<String, LlmResumeAnalysisResponse> evidenceSink) {
        if (config.schemaVersion() != 1 || !com.ororura.analyzer.analysis.domain.ScoringPolicy.VERSION.equals(
                config.scoringPolicy().algorithmVersion())) throw new IllegalArgumentException("Unsupported config version");
        return analyze(file, config.provider(), null, VacancyAnalysisRequest.autoMarket(),
                UUID.randomUUID().toString(), clock.instant(), config, evidenceSink);
    }

    private ResumeAnalysisResult analyze(MultipartFile file, AiProviderType requestedProvider,
            ResumeAnalysisProfile requestedProfile, VacancyAnalysisRequest vacancyAnalysis,
            String analysisId, Instant started, com.ororura.analyzer.analysis.domain.EffectiveAnalysisConfig config,
            java.util.function.BiConsumer<String, LlmResumeAnalysisResponse> evidenceSink) {
        byte[] pdf = fileValidator.validate(file);
        log.info("pdf validated analysisId={} fileSize={}", analysisId, pdf.length);
        String text = textExtractor.extract(pdf);
        log.info("pdf text extracted analysisId={} textLength={}", analysisId, text.length());
        if (text.length() > properties.maxTextLength()) {
            throw new ResumeAnalysisException(ResumeErrorCode.RESUME_TEXT_TOO_LARGE,
                    "Extracted resume text exceeds the configured size limit");
        }
        AiProvider aiProvider = aiProviderRegistry.get(config == null ? requestedProvider : config.provider());
        if (config != null && !java.util.Objects.equals(config.model(), aiProvider.model().orElse(null)))
            throw new IllegalStateException("Pinned provider model changed");
        ResumeAnalysisProfile profile = requestedProfile == null
                ? ResumeAnalysisProfile.defaultProfile() : requestedProfile;
        var resolvedMarketProfile = config == null ? marketProfileProvider.resolve(profile)
                : new com.ororura.analyzer.resume.market.ResolvedMarketAnalysisProfile(config.analysisProfile(),
                        com.ororura.analyzer.resume.market.MarketProfileSource.SNAPSHOT);
        MarketAnalysisProfile marketProfile = resolvedMarketProfile.profile();
        VacancyAnalysisMode mode = vacancyAnalysis == null || vacancyAnalysis.mode() == null
                ? VacancyAnalysisMode.AUTO_MARKET : vacancyAnalysis.mode();
        VacancyMarketData market = config != null ? config.market() : mode == VacancyAnalysisMode.AUTO_MARKET
                ? marketService.load(profile, marketProfile.targetRole())
                : marketService.load(profile, vacancyAnalysis, marketProfile.targetRole());
        if (mode == VacancyAnalysisMode.SINGLE_VACANCY) {
            marketProfile = withVacancyRequirements(marketProfile, market);
        }
        log.info("market context resolved analysisId={} source={} sampleSize={}",
                analysisId, market.source(), market.sampleSize());
        LlmResumeAnalysisResponse llm = config == null ? aiProvider.analyze(marketProfile, text, market) : aiProvider.analyze(config, text);
        evidenceSink.accept(text, llm);
        log.info("AI request completed analysisId={} provider={}", analysisId, aiProvider.type());
        var validator = config == null ? llmValidator : new LlmResponseValidator(
                Clock.fixed(config.evaluationTime(), java.time.ZoneOffset.UTC));
        List<EmploymentPeriod> periods = validator.validateAndConvert(llm, marketProfile);
        log.info("LLM response validated analysisId={}", analysisId);

        ExperienceSummary experience = experienceCalculator.calculate(periods);
        int commercialScore = experienceCalculator.commercialScore(experience.commercialMonths());
        TechnologyProfile technologies = taxonomy.profile(marketProfile, text, llm.skills().confirmed(),
                llm.skills().weakEvidence(), llm.skills().missing());
        AtsScore ats = atsCalculator.calculate(llm.resumeAssessment().atsReadability().score(),
                llm.resumeAssessment().resumeQuality().score(),
                llm.experienceAssessment().experienceDescriptionQuality().score(),
                commercialScore, technologies, marketProfile);
        int technicalScore = overallCalculator.technicalScore(llm.assessments(), marketProfile.criteria());
        int overall = overallCalculator.overall(technicalScore, ats.total(), commercialScore,
                llm.resumeAssessment().resumeQuality().score() * 10,
                llm.experienceAssessment().responsibilityLevel().score());
        int strength = strengthCalculator.calculate(technicalScore, commercialScore,
                llm.experienceAssessment().responsibilityLevel().score());
        var level = levelPolicy.detect(experience.commercialMonths(), technicalScore,
                llm.experienceAssessment().responsibilityLevel().score(), overall);
        var hrChance = chancePolicy.hr(ats.total(), overall);
        var technicalChance = chancePolicy.technical(technicalScore, overall);
        var engine = config == null ? deterministicEngine : defaultEngine(
                new AtsScoreCalculator(com.ororura.analyzer.resume.config.ResumeScoringProperties.fromSnapshot(config.scoringPolicy().parameters())),
                overallCalculator, com.ororura.analyzer.resume.config.ResumeScoringProperties.fromSnapshot(config.scoringPolicy().parameters()));
        if (config != null) level = new com.ororura.analyzer.analysis.domain.GradePolicy().detect(
                experience.commercialMonths(), technicalScore, llm.experienceAssessment().responsibilityLevel().score(), overall).toLegacy();
        var details = engine.analyze(marketProfile, llm, text, market, commercialScore,
                experience.commercialMonths(), ats, technicalScore, level, mode == VacancyAnalysisMode.SINGLE_VACANCY,
                config == null ? null : config.profile().targetGrade());
        Instant completed = clock.instant();
        log.info("deterministic calculations completed analysisId={} ats={} overall={}", analysisId, ats.total(), overall);
        ResumeAnalysisResult result = assembler.assemble(marketProfile, llm, experience, technologies, market, commercialScore,
                ats.total(), overall, strength, level, hrChance, technicalChance, completed,
                aiProvider.type(), aiProvider.model().orElse(null), resolvedMarketProfile.source(), details);
        log.info("resume analysis completed analysisId={} durationMs={}", analysisId,
                Duration.between(started, completed).toMillis());
        return result;
    }

    private static DeterministicAnalysisEngine defaultEngine(AtsScoreCalculator ats,
            OverallScoreCalculator overall) {
        return defaultEngine(ats, overall, new com.ororura.analyzer.resume.config.ResumeScoringProperties());
    }

    private static DeterministicAnalysisEngine defaultEngine(AtsScoreCalculator ats, OverallScoreCalculator overall,
            com.ororura.analyzer.resume.config.ResumeScoringProperties scoring) {
        var catalog = new com.ororura.analyzer.resume.ai.DefaultAnalysisTechnologyCatalog();
        var evidence = new com.ororura.analyzer.resume.domain.EvidenceClassifier(
                new com.ororura.analyzer.resume.domain.SkillNormalizer(catalog));
        return new DeterministicAnalysisEngine(evidence,
                new com.ororura.analyzer.resume.domain.TechnicalProfileScorer(overall),
                new com.ororura.analyzer.resume.domain.GradeFitScorer(),
                new com.ororura.analyzer.resume.domain.MarketFitScorer(scoring),
                new com.ororura.analyzer.resume.domain.SkillGapScorer(scoring),
                new com.ororura.analyzer.resume.domain.SkillRoiCalculator(), ats,
                new com.ororura.analyzer.resume.domain.ClaimRiskAnalyzer(),
                new com.ororura.analyzer.resume.domain.VacancyFitScorer(),
                new com.ororura.analyzer.resume.domain.RiskDeduplicator(),
                new com.ororura.analyzer.resume.domain.RecommendationRanker(),
                new com.ororura.analyzer.resume.domain.MarketPercentileCalculator());
    }

    private static MarketAnalysisProfile withVacancyRequirements(MarketAnalysisProfile profile,
            VacancyMarketData market) {
        java.util.Map<String, com.ororura.analyzer.resume.market.MarketRequirement> requirements =
                new java.util.LinkedHashMap<>();
        profile.requirements().forEach(value -> requirements.put(value.label().toLowerCase(java.util.Locale.ROOT), value));
        market.skillStatistics().forEach(value -> requirements.putIfAbsent(
                value.displayName().toLowerCase(java.util.Locale.ROOT),
                new com.ororura.analyzer.resume.market.MarketRequirement("technology:" + value.normalizedSkill().replace('_', '-'),
                        value.displayName(), com.ororura.analyzer.resume.market.RequirementType.TECHNOLOGY,
                        value.frequency())));
        java.util.Map<String, com.ororura.analyzer.resume.market.MarketSkillStatistics> statistics =
                new java.util.LinkedHashMap<>();
        profile.skillStatistics().forEach(value -> statistics.put(value.displayName().toLowerCase(java.util.Locale.ROOT), value));
        market.skillStatistics().forEach(value -> statistics.put(value.displayName().toLowerCase(java.util.Locale.ROOT), value));
        return new MarketAnalysisProfile(profile.profile(), profile.targetRole(), profile.criteria(),
                java.util.List.copyOf(requirements.values()), java.util.List.copyOf(statistics.values()),
                profile.vacancyRequirements(), profile.sufficientSample(), profile.sampleSize(),
                profile.generatedAt(), profile.version());
    }
}
