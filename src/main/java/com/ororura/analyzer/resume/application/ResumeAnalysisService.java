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
import com.ororura.analyzer.resume.pdf.PdfFileValidator;
import com.ororura.analyzer.resume.pdf.PdfTextExtractor;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.vacancy.VacancyMarketService;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import com.ororura.analyzer.vacancy.VacancySelectionException;
import com.ororura.analyzer.vacancy.hh.VacancySourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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

    public ResumeAnalysisService(PdfFileValidator fileValidator, PdfTextExtractor textExtractor,
            VacancyMarketService marketService, AiProviderRegistry aiProviderRegistry, LlmResponseValidator llmValidator,
            ExperienceCalculator experienceCalculator, TechnologyTaxonomy taxonomy, AtsScoreCalculator atsCalculator,
            OverallScoreCalculator overallCalculator, CandidateStrengthCalculator strengthCalculator,
            CandidateLevelPolicy levelPolicy, InterviewChancePolicy chancePolicy, ResumeAnalysisAssembler assembler,
            ResumeAnalysisProperties properties, Clock clock) {
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
    }

    public ResumeAnalysisResult analyze(MultipartFile file) {
        return analyze(file, null);
    }

    public ResumeAnalysisResult analyze(MultipartFile file, AiProviderType requestedProvider) {
        return analyze(file, requestedProvider, VacancyAnalysisRequest.autoMarket());
    }

    public ResumeAnalysisResult analyze(MultipartFile file, AiProviderType requestedProvider,
            VacancyAnalysisRequest vacancyAnalysis) {
        String analysisId = UUID.randomUUID().toString();
        Instant started = clock.instant();
        log.info("resume analysis started analysisId={} fileSize={}", analysisId, file == null ? 0 : file.getSize());
        try {
            return analyze(file, requestedProvider, vacancyAnalysis, analysisId, started);
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

    private ResumeAnalysisResult analyze(MultipartFile file, AiProviderType requestedProvider,
            VacancyAnalysisRequest vacancyAnalysis, String analysisId, Instant started) {
        byte[] pdf = fileValidator.validate(file);
        log.info("pdf validated analysisId={} fileSize={}", analysisId, pdf.length);
        String text = textExtractor.extract(pdf);
        log.info("pdf text extracted analysisId={} textLength={}", analysisId, text.length());
        if (text.length() > properties.maxTextLength()) {
            throw new ResumeAnalysisException(ResumeErrorCode.RESUME_TEXT_TOO_LARGE,
                    "Extracted resume text exceeds the configured size limit");
        }
        AiProvider aiProvider = aiProviderRegistry.get(requestedProvider);
        VacancyAnalysisMode mode = vacancyAnalysis == null || vacancyAnalysis.mode() == null
                ? VacancyAnalysisMode.AUTO_MARKET : vacancyAnalysis.mode();
        VacancyMarketData market = mode == VacancyAnalysisMode.AUTO_MARKET
                ? marketService.load(properties.targetRole())
                : marketService.load(vacancyAnalysis, properties.targetRole());
        log.info("market context resolved analysisId={} source={} sampleSize={}",
                analysisId, market.source(), market.sampleSize());
        LlmResumeAnalysisResponse llm = aiProvider.analyze(text, market);
        log.info("AI request completed analysisId={} provider={}", analysisId, aiProvider.type());
        List<EmploymentPeriod> periods = llmValidator.validateAndConvert(llm);
        log.info("LLM response validated analysisId={}", analysisId);

        ExperienceSummary experience = experienceCalculator.calculate(periods);
        int commercialScore = experienceCalculator.commercialScore(experience.commercialMonths());
        TechnologyProfile technologies = taxonomy.profile(text, llm.skills().confirmed(),
                llm.skills().weakEvidence(), llm.skills().missing());
        AtsScore ats = atsCalculator.calculate(llm.resumeAssessment().atsReadability().score(),
                llm.resumeAssessment().resumeQuality().score(),
                llm.experienceAssessment().experienceDescriptionQuality().score(),
                commercialScore, technologies, market);
        LlmResumeAnalysisResponse.TechnicalAssessment technical = llm.technicalAssessment();
        int technicalScore = overallCalculator.technicalScore(technical.javaDepth().score(),
                technical.springDepth().score(), technical.backendDepth().score(),
                technical.sqlPostgresqlDepth().score(), technical.hibernateJpaDepth().score(),
                technical.infrastructureDepth().score(), technical.messagingCacheDepth().score(),
                technical.testingDepth().score());
        int overall = overallCalculator.overall(technicalScore, ats.total(), commercialScore,
                llm.resumeAssessment().resumeQuality().score() * 10,
                llm.experienceAssessment().responsibilityLevel().score());
        int strength = strengthCalculator.calculate(technicalScore, commercialScore,
                llm.experienceAssessment().responsibilityLevel().score());
        var level = levelPolicy.detect(experience.commercialMonths(), technicalScore,
                llm.experienceAssessment().responsibilityLevel().score(), overall);
        var hrChance = chancePolicy.hr(ats.total(), overall);
        var technicalChance = chancePolicy.technical(technicalScore, overall);
        Instant completed = clock.instant();
        log.info("deterministic calculations completed analysisId={} ats={} overall={}", analysisId, ats.total(), overall);
        ResumeAnalysisResult result = assembler.assemble(llm, experience, technologies, market, commercialScore,
                ats.total(), overall, strength, level, hrChance, technicalChance, completed,
                aiProvider.type(), aiProvider.model().orElse(null));
        log.info("resume analysis completed analysisId={} durationMs={}", analysisId,
                Duration.between(started, completed).toMillis());
        return result;
    }
}
